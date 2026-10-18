package com.jdinohaila.studentgarden.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdinohaila.studentgarden.R
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun LiveRecordingStatus(startedAt: Long, onStop: () -> Unit) {
    var elapsed by remember(startedAt) { mutableLongStateOf(0L) }
    LaunchedEffect(startedAt) {
        while (true) {
            elapsed = (SystemClock.elapsedRealtime() - startedAt).coerceIn(0, 180_000)
            delay(250)
        }
    }
    RecordingStatus(elapsed, onStop)
}

/** Pinned in the editor footer while the microphone is actually recording. */
@Composable
fun RecordingStatus(elapsedMillis: Long, onStop: () -> Unit) {
    val seconds = elapsedMillis.coerceIn(0, 180_000) / 1000
    Column(Modifier.fillMaxWidth().testTag("recording-status"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.error, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.recording_active), Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.titleMedium)
            Text(String.format(Locale.ROOT, "%02d:%02d / 03:00", seconds / 60, seconds % 60), style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(progress = { seconds / 180f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error)
        Button(onClick = onStop, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
            Icon(Icons.Outlined.Stop, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.stop_recording))
        }
    }
}
