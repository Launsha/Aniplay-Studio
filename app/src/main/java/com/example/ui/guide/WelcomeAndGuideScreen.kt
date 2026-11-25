package com.example.ui.guide

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.scanner.MediaStoreScanner
import com.example.ui.theme.AnimePlayRoleShapes
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDivider
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnPrimaryBlack
import com.example.ui.theme.SecondaryGray
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VlcOrange
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

  val sdVolumes = remember {
    MediaStoreScanner.detectAvailableStorageVolumes(context)
      .filter { !it.isPrimary || it.isRemovable }
  }

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

  val volumeTreeIntentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val uri = result.data?.data
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
              .clip(AnimePlayRoleShapes.Poster)
          )
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
          )
        }
        TextButton(
          onClick = onComplete,
          shape = AnimePlayRoleShapes.Button,
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
            Image(
              painter = painterResource(id = R.drawable.img_welcome_hero),
              contentDescription = stringResource(R.string.cd_welcome_illustration),
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(AnimePlayRoleShapes.Poster)
            )
          } else {
            Surface(
              color = ObsidianSurface,
              shape = CircleShape,
              modifier = Modifier.size(112.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = page.icon,
                  contentDescription = null,
                  tint = TextPrimary,
                  modifier = Modifier.size(48.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(28.dp))

          Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
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
              shape = AnimePlayRoleShapes.Button,
              border = BorderStroke(1.dp, ObsidianBorder),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .scaleOnPress(folderInteraction)
                .testTag("onboarding_pick_folder_button")
            ) {
              Icon(
                imageVector = Icons.Rounded.CreateNewFolder,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.onboarding_pick_folder_cta),
                color = TextPrimary,
                style = MaterialTheme.typography.labelLarge
              )
            }
            if (sdVolumes.isNotEmpty()) {
              Spacer(modifier = Modifier.height(10.dp))
              sdVolumes.forEach { volume ->
                val sdInteraction = remember(volume.id) { MutableInteractionSource() }
                OutlinedButton(
                  onClick = {
                    val intent = volume.openDocumentTreeIntent
                    if (intent != null) {
                      volumeTreeIntentLauncher.launch(intent)
                    } else {
                      folderPickerLauncher.launch(null)
                    }
                  },
                  interactionSource = sdInteraction,
                  shape = AnimePlayRoleShapes.Button,
                  border = BorderStroke(1.dp, ObsidianBorder),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(48.dp)
                    .scaleOnPress(sdInteraction)
                    .testTag("onboarding_pick_sd_button_${volume.id}")
                ) {
                  Icon(
                    imageVector = Icons.Rounded.SdCard,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = stringResource(R.string.settings_pick_sd_card, volume.label),
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }

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
          containerColor = TextPrimary,
          contentColor = OnPrimaryBlack
        ),
        shape = AnimePlayRoleShapes.Button,
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
          titleContentColor = TextPrimary
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
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .testTag("user_guide_list")
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Image(
            painter = painterResource(id = R.drawable.img_welcome_hero),
            contentDescription = stringResource(R.string.cd_welcome_illustration),
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(16f / 9f)
              .clip(AnimePlayRoleShapes.Poster)
          )
          Text(
            text = stringResource(R.string.onboarding_page1_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
          )
          Text(
            text = stringResource(R.string.onboarding_page1_body),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryGray
          )
        }
      }

      item { HorizontalDivider(color = ObsidianDivider) }

      item {
        GuideSection(
          icon = Icons.Rounded.FolderOpen,
          title = stringResource(R.string.guide_naming_title),
          body = stringResource(R.string.guide_naming_body)
        )
      }

      item { HorizontalDivider(color = ObsidianDivider) }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.TouchApp,
              contentDescription = null,
              tint = TextPrimary
            )
            Text(
              text = stringResource(R.string.guide_gestures_title),
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary
            )
          }
          GuideBullet(Icons.Rounded.BrightnessHigh, stringResource(R.string.guide_gesture_left))
          GuideBullet(Icons.Rounded.VolumeUp, stringResource(R.string.guide_gesture_right))
          GuideBullet(Icons.Rounded.FastForward, stringResource(R.string.guide_gesture_double))
          GuideBullet(Icons.Rounded.Lock, stringResource(R.string.guide_gesture_lock))
        }
      }

      item { HorizontalDivider(color = ObsidianDivider) }

      item {
        GuideSection(
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
          shape = AnimePlayRoleShapes.Button,
          border = BorderStroke(1.dp, ObsidianBorder),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .scaleOnPress(replayInteraction)
        ) {
          Icon(
            imageVector = Icons.Rounded.Replay,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.guide_replay_welcome),
            color = TextPrimary,
            style = MaterialTheme.typography.labelLarge
          )
        }
      }
    }
  }
}

@Composable
private fun GuideSection(
  icon: ImageVector,
  title: String,
  body: String
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = TextPrimary)
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary
      )
    }
    Text(
      text = body,
      style = MaterialTheme.typography.bodyMedium,
      color = SecondaryGray
    )
  }
}

@Composable
private fun GuideBullet(icon: ImageVector, text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = SecondaryGray,
      modifier = Modifier.size(18.dp)
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = TextPrimary
    )
  }
}
