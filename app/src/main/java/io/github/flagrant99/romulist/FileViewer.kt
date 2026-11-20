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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.graphics.createBitmap
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewer(
    file: File,
    onBack: () -> Unit
) {
    var currentFile by remember { mutableStateOf(file) }
    val extension = currentFile.extension.lowercase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFile.name, style = MaterialTheme.typography.titleMedium) },
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
                    PdfViewer(currentFile)
                }
                "xml", "html", "txt", "cfg", "log" -> {
                    HtmlViewer(currentFile)
                }
                "png", "jpg", "jpeg" -> {
                    ImageViewer(
                        file = currentFile,
                        onFileChange = { newFile -> currentFile = newFile }
                    )
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
fun ImageViewer(
    file: File,
    onFileChange: (File) -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(file) {
        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
    }

    // Swipe detection logic
    val swipeThreshold = 50
    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(file) {
                detectDragGestures(
                    onDragStart = { offset -> offsetX = offset.x },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    },
                    onDragEnd = {
                        if (offsetX > swipeThreshold) {
                            // Swipe Right -> Next
                            val files = file.parentFile?.listFiles()
                                ?.filter { it.extension.lowercase() in listOf("png", "jpg", "jpeg") }
                                ?.sortedBy { it.name }
                            val currentIndex = files?.indexOf(file) ?: -1
                            if (currentIndex != -1 && files != null && currentIndex < files.size - 1) {
                                onFileChange(files[currentIndex + 1])
                            }
                        } else if (offsetX < -swipeThreshold) {
                            // Swipe Left -> Previous
                            val files = file.parentFile?.listFiles()
                                ?.filter { it.extension.lowercase() in listOf("png", "jpg", "jpeg") }
                                ?.sortedBy { it.name }
                            val currentIndex = files?.indexOf(file) ?: -1
                            if (currentIndex != -1 && files != null && currentIndex > 0) {
                                onFileChange(files[currentIndex - 1])
                            }
                        }
                        offsetX = 0f
                    }
                )
            }
            .padding(0.dp) // Explicitly ensure no extra padding is added by Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Image",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Failed to load image", color = Color.Red)
            }
        }
    }
}
