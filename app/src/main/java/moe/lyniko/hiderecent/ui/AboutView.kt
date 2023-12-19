package moe.lyniko.hiderecent.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.lyniko.hiderecent.R


@Composable
fun AboutView() {
    // center a clickable to open project URL
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // card
            val context = LocalContext.current
            Surface(
                shape = MaterialTheme.shapes.medium, // 使用 MaterialTheme 自带的形状
                shadowElevation = 5.dp,
                modifier = Modifier
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                onClick = {
                    // open project URL
                    val browserIntent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.project_url))
                        )
                    context.startActivity(browserIntent)
                }
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(all = 8.dp)
                    ) {
                        // url notice
                        Text(
                            text = "URL",
                            textAlign = TextAlign.Center
                        )
                    }
                    Row(
                        modifier = Modifier.padding(all = 8.dp)
                    ) {
                        // url
                        Text(
                            text = LocalContext.current.getString(R.string.project_url),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}