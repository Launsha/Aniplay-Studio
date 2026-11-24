package com.example

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.navigation.AnimePlayNavGraph
import com.example.ui.permission.PermissionGateScreen
import com.example.ui.theme.AnimePlayTheme
import com.example.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    )

    setContent {
      AnimePlayTheme {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        var hasPermission by remember {
          mutableStateOf(PermissionUtils.hasVideoPermission(context))
        }
        var wasDenied by remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
          hasPermission = granted
          if (!granted) {
            wasDenied = true
          }
        }

        // Re-check permission when user returns from App Settings
        DisposableEffect(lifecycleOwner) {
          val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
              hasPermission = PermissionUtils.hasVideoPermission(context)
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
          }
        }

        if (hasPermission) {
          AnimePlayNavGraph()
        } else {
          PermissionGateScreen(
            wasDenied = wasDenied,
            onRequestPermission = {
              permissionLauncher.launch(PermissionUtils.requiredVideoPermission())
            },
            onOpenSettings = {
              val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
              )
              context.startActivity(intent)
            }
          )
        }
      }
    }
  }
}
