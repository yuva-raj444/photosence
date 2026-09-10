package com.example.clipgallery.ui

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import kotlinx.coroutines.withContext

private val Canvas = Color(0xFFF5F5F7)
private val Ink = Color(0xFF17171A)
private val Muted = Color(0xFF77777D)
private val Blue = Color(0xFF1677E8)
private val Green = Color(0xFF32C759)
private val Panel = Color.White

@Composable
fun AppleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Blue,
            background = Canvas,
            surface = Panel,
            onBackground = Ink,
            onSurface = Ink
        ),
        content = content
    )
}

@Composable
fun AlbumsScreen(
    albums: List<AlbumInfoUi>,
    onAlbumClick: (AlbumInfoUi) -> Unit,
    onSearch: (String) -> Unit,
    onIndex: () -> Unit,
    onScope: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    AppleScaffold {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Albums", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreHoriz, "More") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Index Album") }, onClick = { menuOpen = false; onIndex() })
                    DropdownMenuItem(text = { Text("Search Scope") }, onClick = { menuOpen = false; onScope() })
                }
            }
        }
        SearchPill(query, { query = it }, { onSearch(query) }, "Search \"golden hour dog\", \"beach trip\", or emotions...", showAdd = false)
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("My Albums", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Badge("${albums.size}")
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        val display = albums.ifEmpty { sampleAlbums }
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(display) { album: AlbumInfoUi -> AlbumTile(album) { onAlbumClick(album) } }
        }
    }
}

@Composable
fun SearchScopeScreen(sources: List<SearchSourceUi>, onToggle: (Long, Boolean) -> Unit, onDone: () -> Unit) {
    AppleSheet(title = "Search Scope", leading = null, trailing = "Done", onTrailing = onDone) {
        Text("Control which indexed directories are included when performing semantic natural language queries.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
        SectionLabel("ACTIVE SEARCH SOURCES", "${sources.count { it.enabled }} active")
        GroupedPanel { sources.forEach { source -> ToggleRow(source.name, source.detail, source.enabled) { value -> onToggle(source.id, value) } } }
        PrivacyCard()
    }
}

@Composable
fun IndexFoldersScreen(folders: List<IndexFolderUi>, selectedIds: Set<Long>, status: String, onDone: () -> Unit, onIndex: () -> Unit, onSelectionChanged: (Set<Long>) -> Unit) {
    val selectedPhotos = folders.filter { it.id in selectedIds }.sumOf { it.count }
    AppleSheet(
        title = "Index Folders",
        leading = "Select All",
        trailing = "Done",
        onTrailing = onDone,
        onLeading = { onSelectionChanged(folders.map { it.id }.toSet()) },
        footer = {
            Text(status, Modifier.fillMaxWidth().padding(bottom = 8.dp), color = Muted, fontSize = 12.sp, maxLines = 1)
            PrimaryButton("Index Selected Folders (${selectedIds.size} selected · $selectedPhotos Photos)", onIndex)
            Text("Indexed with on-device inference · Zero Cloud Upload", Modifier.fillMaxWidth().padding(12.dp), color = Muted, fontSize = 12.sp)
        }
    ) {
        Text("Select local device folders to build high-performance vector semantic search embeddings on-device.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
        PrivateBadge()
        GroupedPanel { folders.forEach { folder -> FolderRow(folder.name, "${folder.count} photos", folder.id in selectedIds) { checked -> onSelectionChanged(if (checked) selectedIds + folder.id else selectedIds - folder.id) } } }
        Text("INDEX FOOTPRINT", Modifier.padding(top = 22.dp), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("$selectedPhotos photos selected", fontWeight = FontWeight.SemiBold); Text("${selectedIds.size} folders", color = Muted) }
        ProgressStrip(if (folders.isEmpty()) 0f else selectedIds.size.toFloat() / folders.size)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
fun SearchResultsScreen(results: List<Uri>, query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit, onCancel: () -> Unit, onPhotoClick: (Int) -> Unit = {}) {
    AppleScaffold {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Search", fontSize = 34.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("Cancel", color = Blue, modifier = Modifier.clickable { onCancel() }) }
        SearchPill(query, onQueryChange, onSearch, "Search photos...", showAdd = false)
        Text("${results.size} semantic matches", Modifier.padding(vertical = 18.dp), color = Ink, fontWeight = FontWeight.SemiBold)
        Text("${results.size} results", Modifier.padding(bottom = 10.dp), color = Muted, fontSize = 13.sp)
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { items(results) { uri: Uri -> PhotoThumb(uri, Modifier.aspectRatio(1f).clickable { onPhotoClick(results.indexOf(uri)) }) } }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lock, null, Modifier.size(13.dp), tint = Muted); Text("Searched ${results.size} indexed photos via on-device inference", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 5.dp)) }
    }
}

@Composable
fun GalleryGridScreen(title: String, photos: List<Uri>, onPhotoClick: (Int) -> Unit) {
    AppleScaffold {
        if (title.isNotBlank()) Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(top = if (title.isBlank()) 0.dp else 14.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(photos, key = { it.toString() }) { uri -> PhotoThumb(uri, Modifier.aspectRatio(1f).clickable { onPhotoClick(photos.indexOf(uri)) }) }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PhotoViewerScreen(photos: List<Uri>, initialIndex: Int, onClose: () -> Unit) {
    val pager = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            ViewerPhoto(photos[page])
        }
        Text("Done", color = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(18.dp).clickable { onClose() }, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AppleScaffold(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize().background(Canvas).padding(horizontal = 16.dp, vertical = 14.dp), content = content) }

@Composable
private fun AppleSheet(title: String, leading: String?, trailing: String, onTrailing: () -> Unit, onLeading: () -> Unit = {}, footer: @Composable ColumnScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize().background(Canvas).padding(horizontal = 16.dp)) { Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) { Box(Modifier.width(38.dp).height(5.dp).clip(CircleShape).background(Color(0xFFD1D1D6))) }; Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text(leading ?: "", color = Blue, modifier = Modifier.clickable { onLeading() }); Spacer(Modifier.weight(1f)); Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(trailing, color = Blue, modifier = Modifier.clickable { onTrailing() }) }; LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = { item { Column(content = content) } }); Column(Modifier.fillMaxWidth().background(Canvas), content = footer) } }

@Composable
private fun SearchPill(value: String, onValue: (String) -> Unit, onSearch: () -> Unit, hint: String, showAdd: Boolean) { OutlinedTextField(value, onValue, Modifier.fillMaxWidth().height(54.dp), placeholder = { Text(hint, color = Muted, maxLines = 1) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) }, trailingIcon = { Row { if (value.isNotEmpty()) IconButton(onClick = { onValue("") }) { Icon(Icons.Default.Close, "Clear") }; IconButton(onClick = onSearch) { Icon(Icons.Default.AutoAwesome, "Search with AI", tint = Blue) }; if (showAdd) IconButton(onClick = {}) { Icon(Icons.Default.Add, "Add") } } }, shape = RoundedCornerShape(18.dp), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSearch() }), colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)) }

@Composable
private fun AlbumTile(album: AlbumInfoUi, onClick: () -> Unit) { Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Panel).clickable { onClick() }) { PhotoThumb(album.cover, Modifier.fillMaxWidth().aspectRatio(1.18f)); Text(album.name, Modifier.padding(start = 10.dp, top = 8.dp), fontWeight = FontWeight.SemiBold); Text("${album.count}", Modifier.padding(start = 10.dp, bottom = 10.dp), color = Muted, fontSize = 13.sp) } }

@Composable
private fun PhotoCard(uri: Uri, title: String, subtitle: String) { Box(Modifier.fillMaxWidth().height(235.dp).clip(RoundedCornerShape(18.dp))) { PhotoThumb(uri, Modifier.fillMaxSize()); Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha = .48f)).padding(14.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(subtitle, color = Color.White.copy(alpha = .9f), fontSize = 12.sp) } } }

@Composable
private fun PhotoThumb(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, uri) { value = withContext(kotlinx.coroutines.Dispatchers.IO) { decodeThumbnail(context, uri) } }
    val loadedBitmap = bitmap
    if (loadedBitmap != null) Image(loadedBitmap.asImageBitmap(), null, modifier.clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
    else Box(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFE3E3E8)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, null, tint = Color.White) }
}

private fun decodeThumbnail(context: android.content.Context, uri: Uri) = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    val sample = if (largest <= 512) 1 else generateSequence(1) { it * 2 }
        .takeWhile { largest / it > 512 }
        .lastOrNull() ?: 1
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()

@Composable
private fun ViewerPhoto(uri: Uri) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, uri) {
        value = withContext(kotlinx.coroutines.Dispatchers.IO) { decodeViewerImage(context, uri) }
    }
    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            loadedBitmap.asImageBitmap(),
            null,
            Modifier.fillMaxSize().background(Color.Black),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Image, null, tint = Color.White)
        }
    }
}

private fun decodeViewerImage(context: android.content.Context, uri: Uri): android.graphics.Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    val sample = if (largest <= 2048) 1 else generateSequence(1) { it * 2 }
        .takeWhile { largest / it > 2048 }
        .lastOrNull() ?: 1
    val decoded = BitmapFactory.Options().apply { inSampleSize = sample }.let { options ->
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } ?: return@runCatching null

    val orientation = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        ?: ExifInterface.ORIENTATION_NORMAL
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
        }
    }
    if (matrix.isIdentity) decoded else android.graphics.Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
        if (it !== decoded) decoded.recycle()
    }
}.getOrNull()

@Composable
private fun GroupedPanel(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel), content = content) }

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFE8F0FF)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Tune, null, Modifier.size(17.dp), tint = Blue) }; Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(subtitle, color = Muted, fontSize = 11.sp) }; Switch(checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Green)) } }

@Composable
private fun FolderRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (title.contains("Video")) Icons.Default.VideoLibrary else Icons.Default.Image, null, tint = Blue, modifier = Modifier.size(23.dp)); Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 12.sp) }; IconButton(onClick = { onChecked(!checked) }, modifier = Modifier.size(30.dp)) { Box(Modifier.size(22.dp).clip(CircleShape).then(if (checked) Modifier.background(Blue) else Modifier.border(1.5.dp, Color(0xFFC7C7CC), CircleShape)), contentAlignment = Alignment.Center) { if (checked) Icon(Icons.Default.Check, null, Modifier.size(15.dp), tint = Color.White) } } } }

@Composable
private fun SectionLabel(label: String, trailing: String? = null) { Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp); trailing?.let { Text(it, color = Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } } }

@Composable
private fun Badge(value: String) { Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFE5E5EA)).padding(horizontal = 7.dp, vertical = 3.dp)) { Text(value, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun SegmentedControl(labels: List<String>, selected: Int) { Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE5E5EA)).padding(3.dp)) { labels.forEachIndexed { index, label -> Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (index == selected) Color.White else Color.Transparent).padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text(label, fontSize = 12.sp, fontWeight = if (index == selected) FontWeight.SemiBold else FontWeight.Normal) } } } }

@Composable
private fun ProgressStrip(fraction: Float) { Row(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFFE5E5EA))) { Box(Modifier.fillMaxWidth(fraction).background(Blue)); Box(Modifier.weight(1f).background(Green)) } }

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Blue).clickable { onClick() }.padding(15.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp)); Text(label, Modifier.padding(start = 8.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }

@Composable
private fun PrivateBadge() { Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFE8F0FF)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lock, null, Modifier.size(13.dp), tint = Blue); Text("Private on-device inference", Modifier.padding(start = 6.dp), color = Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }

@Composable
private fun PrivacyCard() { Row(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFEFFAF0)).padding(12.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.Lock, null, Modifier.size(16.dp), tint = Color(0xFF238636)); Text("Semantic indexing runs private and encrypted on-device. Toggling folders instantly updates search scope.", Modifier.padding(start = 8.dp), color = Color(0xFF35603C), fontSize = 12.sp, lineHeight = 17.sp) } }

data class AlbumInfoUi(val id: Long, val name: String, val cover: Uri, val count: Int)
data class SearchSourceUi(val id: Long, val name: String, val detail: String, val enabled: Boolean)
data class IndexFolderUi(val id: Long, val name: String, val count: Int)

private val sampleAlbums = emptyList<AlbumInfoUi>()
