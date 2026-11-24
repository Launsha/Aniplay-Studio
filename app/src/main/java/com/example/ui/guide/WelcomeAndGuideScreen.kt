package com.example.ui.guide

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.VlcOrange
import com.example.ui.theme.VlcOrangeSoft
import com.example.util.scaleOnPress
import kotlinx.coroutines.launch

private data class OnboardingPage(
  val icon: ImageVector,
  val titleRes: Int,
  val bodyRes: Int,
  val showHeroImage: Boolean = false
)

@Composable
fun WelcomeOnboardingScreen(
  onAddFolderUri: (String) -> Unit,
  onComplete: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val pages = remember {
    listOf(
      OnboardingPage(
        icon = Icons.Rounded.PlayArrow,
        titleRes = R.string.onboarding_page1_title,
        bodyRes = R.string.onboarding_page1_body,
        showHeroImage = true
      ),
      OnboardingPage(
        icon = Icons.Rounded.ViewCarousel,
        titleRes = R.string.onboarding_page2_title,
        bodyRes = R.string.onboarding_page2_body
      ),
      OnboardingPage(
        icon = Icons.Rounded.Tune,
        titleRes = R.string.onboarding_page3_title,
        bodyRes = R.string.onboarding_page3_body
      ),
      OnboardingPage(
        icon = Icons.Rounded.CreateNewFolder,
        titleRes = R.string.onboarding_page4_title,
        bodyRes = R.string.onboarding_page4_body
      )
    )
  }

  val pagerState = rememberPagerState(pageCount = { pages.size })

  val folderPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      }
      onAddFolderUri(uri.toString())
      onComplete()
    }
  }

  Scaffold(
    containerColor = ObsidianBackground,
    contentWindowInsets = WindowInsets.statusBars
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 20.dp, vertical = 16.dp)
        .testTag("welcome_onboarding_root"),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_app_icon_play),
            contentDescription = stringResource(R.string.cd_app_logo),
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(8.dp))
          )
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = VlcOrange
          )
        }
        TextButton(
          onClick = onComplete,
          modifier = Modifier.minimumInteractiveComponentSize()
        ) {
          Text(
            text = stringResource(R.string.action_skip),
            color = SecondaryGray,
            style = MaterialTheme.typography.labelLarge
          )
        }
      }

      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .widthIn(max = 540.dp)
      ) { pageIdx ->
        val page = pages[pageIdx]
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          if (page.showHeroImage) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_welcome_hero),
                contentDescription = stringResource(R.string.cd_welcome_illustration),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(Color.Transparent, ObsidianBackground.copy(alpha = 0.65f))
                    )
                  )
              )
            }
          } else {
            Surface(
              color = ObsidianSurface,
              shape = CircleShape,
              modifier = Modifier
                .size(120.dp)
                .border(2.dp, VlcOrange.copy(alpha = 0.5f), CircleShape)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.background(
                  Brush.radialGradient(
                    colors = listOf(VlcOrangeSoft, ObsidianSurface)
                  )
                )
              ) {
                Icon(
                  imageVector = page.icon,
                  contentDescription = null,
                  tint = VlcOrange,
                  modifier = Modifier.size(54.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(28.dp))

          Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
          )

          if (pageIdx == pages.lastIndex) {
            Spacer(modifier = Modifier.height(22.dp))
            val folderInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
              onClick = { folderPickerLauncher.launch(null) },
              interactionSource = folderInteraction,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .scaleOnPress(folderInteraction)
                .testTag("onboarding_pick_folder_button")
            ) {
              Icon(
                imageVector = Icons.Rounded.CreateNewFolder,
                contentDescription = null,
                tint = VlcOrange,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.onboarding_pick_folder_cta),
                color = VlcOrange,
                style = MaterialTheme.typography.labelLarge
              )
            }
          }
        }
      }

      // Pager indicators
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 18.dp)
      ) {
        repeat(pages.size) { index ->
          val isSelected = pagerState.currentPage == index
          Box(
            modifier = Modifier
              .height(8.dp)
              .width(if (isSelected) 26.dp else 8.dp)
              .clip(CircleShape)
              .background(if (isSelected) VlcOrange else ObsidianSurfaceElevated)
          )
        }
      }

      val ctaInteraction = remember { MutableInteractionSource() }
      val isLastPage = pagerState.currentPage == pages.lastIndex
      Button(
        onClick = {
          if (isLastPage) {
            onComplete()
          } else {
            scope.launch {
              pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
          }
        },
        interactionSource = ctaInteraction,
        colors = ButtonDefaults.buttonColors(
          containerColor = VlcOrange,
          contentColor = OnPrimaryBlack
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 540.dp)
          .height(52.dp)
          .scaleOnPress(ctaInteraction)
          .testTag("onboarding_next_button")
      ) {
        Text(
          text = stringResource(if (isLastPage) R.string.action_start_now else R.string.action_next),
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideScreen(
  onBack: () -> Unit,
  onReplayWelcome: () -> Unit
) {
  Scaffold(
    containerColor = ObsidianBackground,
    contentWindowInsets = WindowInsets.statusBars,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = ObsidianBackground,
          titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("guide_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.action_back)
            )
          }
        },
        title = {
          Text(
            text = stringResource(R.string.guide_screen_title),
            style = MaterialTheme.typography.titleLarge
          )
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .testTag("user_guide_list")
    ) {
      item {
        Surface(
          color = ObsidianSurface,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        ) {
          Column {
            Image(
              painter = painterResource(id = R.drawable.img_welcome_hero),
              contentDescription = stringResource(R.string.cd_welcome_illustration),
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
            )
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = stringResource(R.string.onboarding_page1_title),
                style = MaterialTheme.typography.titleLarge,
                color = VlcOrange
              )
              Text(
                text = stringResource(R.string.onboarding_page1_body),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryGray
              )
            }
          }
        }
      }

      item {
        GuideCard(
          icon = Icons.Rounded.FolderOpen,
          title = stringResource(R.string.guide_naming_title),
          body = stringResource(R.string.guide_naming_body)
        )
      }

      item {
        Surface(
          color = ObsidianSurface,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.TouchApp,
                contentDescription = null,
                tint = VlcOrange
              )
              Text(
                text = stringResource(R.string.guide_gestures_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            GuideBullet(Icons.Rounded.BrightnessHigh, stringResource(R.string.guide_gesture_left))
            GuideBullet(Icons.Rounded.VolumeUp, stringResource(R.string.guide_gesture_right))
            GuideBullet(Icons.Rounded.FastForward, stringResource(R.string.guide_gesture_double))
            GuideBullet(Icons.Rounded.Lock, stringResource(R.string.guide_gesture_lock))
          }
        }
      }

      item {
        GuideCard(
          icon = Icons.Rounded.Star,
          title = stringResource(R.string.guide_mal_title),
          body = stringResource(R.string.guide_mal_body)
        )
      }

      item {
        val replayInteraction = remember { MutableInteractionSource() }
        OutlinedButton(
          onClick = onReplayWelcome,
          interactionSource = replayInteraction,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .scaleOnPress(replayInteraction)
        ) {
          Icon(
            imageVector = Icons.Rounded.Replay,
            contentDescription = null,
            tint = VlcOrange,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.guide_replay_welcome),
            color = VlcOrange,
            style = MaterialTheme.typography.labelLarge
          )
        }
      }
    }
  }
}

@Composable
private fun GuideCard(
  icon: ImageVector,
  title: String,
  body: String
) {
  Surface(
    color = ObsidianSurface,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = VlcOrange)
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = SecondaryGray
      )
    }
  }
}

@Composable
private fun GuideBullet(icon: ImageVector, text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(ObsidianSurfaceElevated)
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = VlcOrange,
      modifier = Modifier.size(18.dp)
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
