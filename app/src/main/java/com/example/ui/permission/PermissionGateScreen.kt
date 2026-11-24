package com.example.ui.permission

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.VlcOrange
import com.example.ui.theme.VlcOrangeSoft
import com.example.util.scaleOnPress

@Composable
fun PermissionGateScreen(
  wasDenied: Boolean,
  onRequestPermission: () -> Unit,
  onOpenSettings: () -> Unit
) {
  val grantInteraction = remember { MutableInteractionSource() }
  val settingsInteraction = remember { MutableInteractionSource() }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 480.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(4f / 3f)
          .clip(RoundedCornerShape(24.dp))
          .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_empty_cinema),
          contentDescription = stringResource(R.string.cd_empty_illustration),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Transparent,
                  ObsidianBackground.copy(alpha = 0.85f)
                )
              )
            )
        )
        Surface(
          color = VlcOrangeSoft,
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.OfflineBolt,
              contentDescription = null,
              tint = VlcOrange,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.labelMedium,
              color = VlcOrange
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Text(
        text = stringResource(R.string.permission_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = stringResource(R.string.permission_description),
        style = MaterialTheme.typography.bodyMedium,
        color = SecondaryGray,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(16.dp))

      Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.FolderOpen,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(22.dp)
          )
          Text(
            text = stringResource(
              if (wasDenied) R.string.permission_denied_hint else R.string.permission_offline_note
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (wasDenied) MaterialTheme.colorScheme.error else SecondaryGray
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Button(
        onClick = onRequestPermission,
        interactionSource = grantInteraction,
        colors = ButtonDefaults.buttonColors(
          containerColor = VlcOrange,
          contentColor = OnPrimaryBlack
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .scaleOnPress(grantInteraction)
          .testTag("grant_permission_button")
      ) {
        Text(
          text = stringResource(R.string.permission_grant_button),
          style = MaterialTheme.typography.labelLarge
        )
      }

      if (wasDenied) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
          onClick = onOpenSettings,
          interactionSource = settingsInteraction,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .scaleOnPress(settingsInteraction)
            .testTag("open_settings_button")
        ) {
          Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = stringResource(R.string.permission_settings_button),
            style = MaterialTheme.typography.labelLarge,
            color = VlcOrange
          )
        }
      }
    }
  }
}
