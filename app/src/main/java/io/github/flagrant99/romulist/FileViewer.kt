package io.github.flagrant99.romulist

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewer(
    file: File,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val extension = file.extension.lowercase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.Green,
                    navigationIconContentColor = Color.Green
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when (extension) {
                "pdf" -> {
                    PdfViewer(file)
                }
                "xml", "html", "txt", "cfg", "log" -> {
                    HtmlViewer(file)
                }
                "png", "jpg", "jpeg" -> {
                    ImageViewer(file)
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Unsupported file type: $extension", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun HtmlViewer(file: File) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.setSupportZoom(true)
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }
                }
                
                // Set dark mode for WebView if possible, or just inject CSS
                setBackgroundColor(0xFF000000.toInt())
            }
        },
        update = { webView ->
            val content = try {
                file.readText()
            } catch (e: Exception) {
                "Error reading file: ${e.message}"
            }
            
            val mimeType = when (file.extension.lowercase()) {
                "xml" -> "text/xml"
                "html" -> "text/html"
                "cfg", "log" -> "text/plain"
                else -> "text/plain"
            }
            
            // Basic styling for dark mode
            val styledContent = """
                <html>
                <head>
                <style>
                    body {
                        background-color: #000000;
                        color: #00FF00;
                        font-family: monospace;
                        padding: 16px;
                    }
                    pre {
                        white-space: pre-wrap;
                        word-wrap: break-word;
                    }
                </style>
                </head>
                <body>
                    <pre>${content.replace("<", "&lt;").replace(">", "&gt;")}</pre>
                </body>
                </html>
            """.trimIndent()
            
            if (file.extension.lowercase() == "html") {
                webView.loadUrl("file://${file.absolutePath}")
            } else {
                webView.loadDataWithBaseURL(null, styledContent, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun PdfViewer(file: File) {
    val context = LocalContext.current
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        try {
            val parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(parcelFileDescriptor)
            val pageCount = renderer.pageCount
            val list = mutableListOf<Bitmap>()
            
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                // Adjust scale for better quality
                val bitmap = createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                list.add(bitmap)
                page.close()
            }
            bitmaps = list
            renderer.close()
            parcelFileDescriptor.close()
        } catch (e: Exception) {
            error = e.message
        }
    }

    if (error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error loading PDF: $error", color = Color.Red)
        }
    } else if (bitmaps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.Green)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(bitmaps) { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PDF Page",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ImageViewer(file: File) {
    val context = LocalContext.current
    val bitmap = remember(file) {
        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
    }

    if (bitmap != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Image",
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Failed to load image", color = Color.Red)
        }
    }
}
