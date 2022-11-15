package utap.tjp2677.antimatter

import android.app.Application
import com.google.android.material.color.DynamicColors

class AntiMatterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic color
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}