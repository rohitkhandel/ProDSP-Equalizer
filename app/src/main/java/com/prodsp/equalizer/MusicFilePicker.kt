
package com.prodsp.equalizer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MusicFilePicker(
    onFileSelected: (android.net.Uri) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onFileSelected(it) }
    }

    Button(
        onClick = {
            launcher.launch(arrayOf("audio/*"))
        }
    ) {
        Text("SELECT MUSIC")
    }
}
