package utap.tjp2677.antimatter.context

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class ProcessTextActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent.getCharArrayExtra(Intent.EXTRA_PROCESS_TEXT)
        Log.d(javaClass.simpleName, "Text: $text")

    }

}