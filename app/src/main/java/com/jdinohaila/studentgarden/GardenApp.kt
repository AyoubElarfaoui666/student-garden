package com.jdinohaila.studentgarden

import android.app.Application
import com.jdinohaila.studentgarden.data.GardenRepository
import com.jdinohaila.studentgarden.platform.PrivateMedia

class GardenApp : Application() {
    val repository by lazy { GardenRepository(this) }
    val media by lazy { PrivateMedia(this) }
}
