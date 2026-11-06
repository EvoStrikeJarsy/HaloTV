package com.halotv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.halotv.app.ui.HaloApp
import com.halotv.app.ui.theme.HaloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HaloTheme { HaloApp() } }
    }
}
