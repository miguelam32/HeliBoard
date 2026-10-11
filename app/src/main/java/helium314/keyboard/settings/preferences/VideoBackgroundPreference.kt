// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.FileUtils
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.IntentUtils
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.dialogs.ConfirmationDialog
import helium314.keyboard.settings.dialogs.InfoDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun VideoBackgroundPref(setting: Setting) {
    var showSelectionDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current
    fun getFile() = Settings.getCustomBackgroundVideoFile(ctx)
    val b = (ctx.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0)
        androidx.compose.runtime.remember { Unit }
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        showSelectionDialog = false
        scope.launch(Dispatchers.IO) {
            if (!setBackgroundVideo(ctx, uri))
                showErrorDialog = true
        }
    }
    val intent = IntentUtils.getResolvableTypeIntent(ctx, "video/*")
        .setAction(Intent.ACTION_OPEN_DOCUMENT)
    Preference(
        name = setting.title,
        onClick = {
            if (!getFile().exists()) {
                launcher.launch(intent)
            } else {
                showSelectionDialog = true
            }
        }
    )
    if (showSelectionDialog) {
        ConfirmationDialog(
            onDismissRequest = { showSelectionDialog = false },
            title = { Text(stringResource(R.string.customize_background_video)) },
            confirmButtonText = stringResource(R.string.button_load_custom),
            onConfirmed = { launcher.launch(intent) },
            neutralButtonText = stringResource(R.string.delete),
            onNeutral = {
                getFile().delete()
                KeyboardSwitcher.getInstance().setThemeNeedsReload()
                showSelectionDialog = false
            }
        )
    }
    if (showErrorDialog) {
        InfoDialog(stringResource(R.string.file_read_error)) { showErrorDialog = false }
    }
}

private fun setBackgroundVideo(ctx: Context, uri: Uri): Boolean {
    val videoFile = Settings.getCustomBackgroundVideoFile(ctx)
    FileUtils.copyContentUriToNewFile(uri, ctx, videoFile)
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoFile.absolutePath)
        retriever.release()
    } catch (_: Exception) {
        videoFile.delete()
        return false
    }
    KeyboardSwitcher.getInstance().setThemeNeedsReload()
    return true
}
