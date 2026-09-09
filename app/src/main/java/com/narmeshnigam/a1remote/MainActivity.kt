package com.narmeshnigam.a1remote

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import com.narmeshnigam.a1remote.ui.A1App

/** The single activity. Everything above it is Compose. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // The soft keyboard is inset, not overlaid: on the Keyboard screen the Send key is
            // the whole point of the screen, and a keyboard covering it makes the screen useless.
            A1App(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding(),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // BUILD_SPEC §5: the screen stays awake while the remote is in front of the user. A
        // remote that blanks mid-film is worse than no remote.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        // Released the moment the app is backgrounded, so the flag never outlives the remote.
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }
}
