package com.jdinohaila.studentgarden.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jdinohaila.studentgarden.domain.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GardenDatabase(context: Context) : SQLiteOpenHelper(context, "garden.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE records(id TEXT PRIMARY KEY, kind TEXT NOT NULL, data TEXT NOT NULL, updated_at INTEGER NOT NULL, trashed_at INTEGER)"
        )
        db.execSQL("CREATE INDEX records_kind ON records(kind,trashed_at)")
        db.execSQL("CREATE TABLE note_contents(id TEXT PRIMARY KEY, html TEXT NOT NULL)")
        db.execSQL(
            "CREATE TABLE habit_checks(habit_id TEXT NOT NULL, day TEXT NOT NULL, checked_at INTEGER NOT NULL, PRIMARY KEY(habit_id,day))"
        )
        db.execSQL("CREATE TABLE timer(id INTEGER PRIMARY KEY CHECK(id=1), data TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        error("Unsupported database upgrade")
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }
}

class GardenRepository(private val context: Context) {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "recordType"
    }
    val database = GardenDatabase(context.applicationContext)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(GardenState())
    val state = _state.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) { mutex.withLock { refresh() } }

    private fun kind(record: GardenRecord) =
        when (record) {
            is Course -> "course"
            is Task -> "task"
            is Note -> "note"
            is StudyBlock -> "block"
            is Deck -> "deck"
            is Flashcard -> "card"
            is Lesson -> "lesson"
            is Habit -> "habit"
            is Review -> "review"
        }

    private fun write(record: GardenRecord) {
        validate(record)
        database.writableDatabase.insertWithOnConflict(
            "records",
            null,
            ContentValues().apply {
                put("id", record.id)
                put("kind", kind(record))
                put("data", json.encodeToString<GardenRecord>(record))
                put("updated_at", System.currentTimeMillis())
                putNull("trashed_at")
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    suspend fun save(record: GardenRecord, html: String? = null) = mutate {
        write(record)
        if (record is Note && html != null) {
            require(html.length <= 1_000_000) { "Note is too large" }
            database.writableDatabase.insertWithOnConflict(
                "note_contents",
                null,
                ContentValues().apply {
                    put("id", record.id)
                    put("html", html)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
    }

    suspend fun trash(id: String) = mutate {
        database.writableDatabase.execSQL(
            "UPDATE records SET trashed_at=? WHERE id=?",
            arrayOf<Any>(System.currentTimeMillis(), id),
        )
    }

    suspend fun undoTrash(id: String) = mutate {
        database.writableDatabase.execSQL(
            "UPDATE records SET trashed_at=NULL WHERE id=?",
            arrayOf(id),
        )
    }

    suspend fun body(id: String): String =
        withContext(Dispatchers.IO) {
            database.readableDatabase
                .rawQuery("SELECT html FROM note_contents WHERE id=?", arrayOf(id))
                .use { if (it.moveToFirst()) it.getString(0) else "" }
        }

    suspend fun rate(card: Flashcard, rating: String) = mutate {
        val current = _state.value.cards.find { it.id == card.id } ?: error("Card no longer exists")
        require(current.stage == card.stage && current.dueAt == card.dueAt) {
            "This card has already been reviewed"
        }
        val now = System.currentTimeMillis()
        write(StudyRules.advance(current, rating, now))
        write(Review(itemId = card.id, kind = "card", rating = rating, createdAt = now))
    }

    suspend fun review(lesson: Lesson) = mutate {
        val current =
            _state.value.lessons.find { it.id == lesson.id } ?: error("Lesson no longer exists")
        require(current.stage == lesson.stage && current.dueAt == lesson.dueAt) {
            "This lesson has already been reviewed"
        }
        write(StudyRules.review(current, System.currentTimeMillis()))
        write(Review(itemId = lesson.id, kind = "lesson", rating = "review"))
    }

    suspend fun check(habit: Habit, day: LocalDate) = mutate {
        require(day <= StudyRules.today(habit) && day >= LocalDate.parse(habit.startDay))
        val db = database.writableDatabase
        val existing =
            db.delete("habit_checks", "habit_id=? AND day=?", arrayOf(habit.id, day.toString()))
        if (existing == 0) {
            require(StudyRules.scheduled(habit, day))
            db.insertOrThrow(
                "habit_checks",
                null,
                ContentValues().apply {
                    put("habit_id", habit.id)
                    put("day", day.toString())
                    put("checked_at", System.currentTimeMillis())
                },
            )
        }
    }

    suspend fun setTimer(timer: FocusTimer?) = mutate { writeTimer(timer) }

    private fun writeTimer(timer: FocusTimer?) {
        database.writableDatabase.delete("timer", null, null)
        if (timer != null)
            database.writableDatabase.insertOrThrow(
                "timer",
                null,
                ContentValues().apply {
                    put("id", 1)
                    put("data", json.encodeToString(timer))
                },
            )
    }

    suspend fun finishTimer(id: String): Boolean =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val timer = readTimer() ?: return@withLock false
                val boot =
                    android.provider.Settings.Global.getInt(
                        context.contentResolver,
                        android.provider.Settings.Global.BOOT_COUNT,
                        0,
                    )
                if (
                    timer.id != id ||
                        timer.finished ||
                        timer.endsAt == null ||
                        StudyRules.remaining(
                            timer,
                            System.currentTimeMillis(),
                            android.os.SystemClock.elapsedRealtime(),
                            boot,
                        ) > 0
                )
                    return@withLock false
                val db = database.writableDatabase
                db.beginTransaction()
                try {
                    writeTimer(
                        timer.copy(finished = true, remaining = 0, endsAt = null, elapsedEnd = null)
                    )
                    write(
                        StudyBlock(
                            id = timer.id,
                            title = "Focus",
                            courseId = timer.courseId,
                            startsAt = timer.startedAt,
                            minutes = timer.minutes,
                            done = true,
                            source = "focus",
                        )
                    )
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                refresh()
                true
            }
        }

    private fun readTimer(): FocusTimer? =
        database.readableDatabase.rawQuery("SELECT data FROM timer WHERE id=1", null).use {
            if (it.moveToFirst()) json.decodeFromString(it.getString(0)) else null
        }

    suspend fun allRecords(): List<GardenRecord> =
        withContext(Dispatchers.IO) { mutex.withLock { readRecords() } }

    suspend fun snapshot(): Triple<List<GardenRecord>, Map<String, String>, List<HabitCheck>> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val records = readRecords()
                val bodies =
                    records.filterIsInstance<Note>().associate { note ->
                        note.id to
                            database.readableDatabase
                                .rawQuery(
                                    "SELECT html FROM note_contents WHERE id=?",
                                    arrayOf(note.id),
                                )
                                .use { if (it.moveToFirst()) it.getString(0) else "" }
                    }
                val checks =
                    database.readableDatabase
                        .rawQuery("SELECT habit_id,day,checked_at FROM habit_checks", null)
                        .use { c ->
                            buildList {
                                while (c.moveToNext()) add(
                                    HabitCheck(c.getString(0), c.getString(1), c.getLong(2))
                                )
                            }
                        }
                        .filter { c -> records.any { it is Habit && it.id == c.habitId } }
                Triple(records, bodies, checks)
            }
        }

    private fun readRecords(): List<GardenRecord> =
        database.readableDatabase
            .rawQuery(
                "SELECT data FROM records WHERE trashed_at IS NULL ORDER BY updated_at DESC",
                null,
            )
            .use { c ->
                buildList {
                    while (c.moveToNext()) add(json.decodeFromString<GardenRecord>(c.getString(0)))
                }
            }

    suspend fun merge(
        records: List<GardenRecord>,
        notes: Map<String, String>,
        checks: List<HabitCheck>,
    ) = mutate {
        records.forEach { r ->
            validate(r)
            database.readableDatabase
                .rawQuery("SELECT id FROM records WHERE id=?", arrayOf(r.id))
                .use { if (!it.moveToFirst()) write(r) }
        }
        notes.forEach { (id, html) ->
            require(html.length <= 1_000_000)
            database.writableDatabase.insertWithOnConflict(
                "note_contents",
                null,
                ContentValues().apply {
                    put("id", id)
                    put("html", html)
                },
                SQLiteDatabase.CONFLICT_IGNORE,
            )
        }
        checks.forEach { c ->
            LocalDate.parse(c.day)
            database.writableDatabase.insertWithOnConflict(
                "habit_checks",
                null,
                ContentValues().apply {
                    put("habit_id", c.habitId)
                    put("day", c.day)
                    put("checked_at", c.checkedAt)
                },
                SQLiteDatabase.CONFLICT_IGNORE,
            )
        }
    }

    private suspend fun mutate(block: () -> Unit) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val db = database.writableDatabase
                db.beginTransaction()
                try {
                    block()
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                refresh()
            }
        }

    private fun refresh() {
        val records = readRecords()
        val checks =
            database.readableDatabase
                .rawQuery("SELECT habit_id,day,checked_at FROM habit_checks", null)
                .use { c ->
                    buildList {
                        while (c.moveToNext()) add(
                            HabitCheck(c.getString(0), c.getString(1), c.getLong(2))
                        )
                    }
                }
        _state.value =
            GardenState(
                records.filterIsInstance<Course>(),
                records.filterIsInstance<Task>(),
                records.filterIsInstance<Note>(),
                records.filterIsInstance<StudyBlock>(),
                records.filterIsInstance<Deck>(),
                records.filterIsInstance<Flashcard>(),
                records.filterIsInstance<Lesson>(),
                records.filterIsInstance<Habit>(),
                records.filterIsInstance<Review>(),
                checks,
                readTimer(),
                true,
                database.readableDatabase
                    .rawQuery(
                        "SELECT data FROM records WHERE trashed_at IS NOT NULL ORDER BY trashed_at DESC",
                        null,
                    )
                    .use { c ->
                        buildList {
                            while (c.moveToNext()) add(
                                json.decodeFromString<GardenRecord>(c.getString(0))
                            )
                        }
                    },
            )
        com.jdinohaila.studentgarden.platform.LessonReminder.schedule(context, _state.value.lessons)
        com.jdinohaila.studentgarden.platform.StudyReminders.schedule(context, _state.value)
    }

    fun validate(r: GardenRecord) {
        require(runCatching { java.util.UUID.fromString(r.id) }.isSuccess) { "Invalid record ID" }
        val title =
            when (r) {
                is Course -> r.title
                is Task -> r.title
                is Note -> r.title
                is StudyBlock -> r.title
                is Deck -> r.title
                is Lesson -> r.title
                is Habit -> r.title
                is Flashcard -> r.answer
                is Review -> r.rating
            }
        require(title.isNotBlank() && title.length <= 10_000) { "Add a title or answer" }
        when (r) {
            is Habit -> {
                require(r.days in 1..127)
                java.time.ZoneId.of(r.zone)
                require(
                    LocalDate.parse(r.startDay) in LocalDate.of(2000, 1, 1)..StudyRules.today(r)
                )
            }
            is Flashcard -> {
                require(r.stage in 0..4 && r.type in setOf("text", "image", "audio"))
                require(r.type != "text" || r.prompt.isNotBlank()) { "Add a question or cue" }
                require(r.type != "image" || r.imageId != null) { "Attach an image" }
                require(r.type != "audio" || r.audioId != null) { "Attach audio" }
                listOfNotNull(r.imageId, r.audioId).forEach {
                    require(it.matches(Regex("[a-f0-9-]{36}\\.[a-z0-9]{1,5}")))
                }
            }
            is Lesson -> require(r.stage in 0..3 && (r.stage == 3) == (r.dueAt == null))
            is Task -> require(r.minutes in 1..1440 && r.status in setOf("todo", "progress", "done") && r.priority in setOf("normal", "high"))
            is StudyBlock -> require(r.minutes in 1..1440)
            else -> Unit
        }
    }
}
