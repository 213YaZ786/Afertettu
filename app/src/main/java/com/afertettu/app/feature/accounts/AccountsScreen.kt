package com.afertettu.app.feature.accounts

import com.afertettu.app.core.model.Actor
import com.afertettu.app.ui.component.BoldIconButton
import com.afertettu.app.ui.component.BoldButton
import com.afertettu.app.ui.theme.zone
import com.afertettu.app.navigation.LocalReadableInset
import com.afertettu.app.ui.component.FolderDialog
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.AssistChip
import com.afertettu.app.ui.component.LocalDockPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afertettu.app.ui.component.Avatar
import com.afertettu.app.ui.component.relativeTime
import com.afertettu.app.ui.icon.AfertettuIcons
import org.koin.androidx.compose.koinViewModel

/**
 * Accounts and search merged. One pill search bar on top: it filters the
 * accounts you follow, searches people on Bluesky by name, and when the text
 * is a handle or a link, a card offers to open that profile or follow it.
 * Typing never follows anyone.
 * Unfollowing happens on the profile, which keeps it away from a stray tap.
 */
@Composable
fun AccountsScreen(
    onOpenFeed: (String) -> Unit,
    onOpenFolders: () -> Unit,
    viewModel: AccountsViewModel = koinViewModel()
) {
    val rows by viewModel.rows.collectAsState()
    val folders by viewModel.folders.collectAsState()
    var filing by remember { mutableStateOf<AccountRow?>(null) }

    filing?.let { row ->
        FolderDialog(
            title = "File @${row.handle}",
            folders = folders,
            selected = row.folder,
            everything = null,
            onSelect = { name ->
                viewModel.setFolder(row.handle, name.orEmpty())
                filing = null
            },
            onDismiss = { filing = null }
        )
    }
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current

    val people by viewModel.people.collectAsState()

    // Coming back from a profile may have brought new posts or an avatar.
    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(query) { viewModel.searchPeople(query) }

    val candidate = AccountsViewModel.asHandle(query)
    // A pasted link filters by the handle it names, so a link to an account
    // already followed finds that account instead of matching nothing.
    val trimmed = if ('/' in query) candidate.orEmpty() else query.trim().removePrefix("@")
    val alreadyFollowed = candidate != null &&
        rows.any { it.handle.equals(candidate, ignoreCase = true) }
    val visible = if (trimmed.isEmpty()) {
        rows
    } else {
        rows.filter {
            it.handle.contains(trimmed, ignoreCase = true) ||
                it.name?.contains(trimmed, ignoreCase = true) == true
        }
    }

    fun open(handle: String) {
        focus.clearFocus()
        onOpenFeed(handle)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                start = 24.dp + LocalReadableInset.current,
                end = 24.dp + LocalReadableInset.current,
                top = 24.dp,
                bottom = 12.dp
            ),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                Text("Accounts", style = MaterialTheme.typography.headlineMedium)
                if (rows.isNotEmpty()) {
                    Text(
                        "${rows.size} followed",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            BoldIconButton(onClick = onOpenFolders) {
                Icon(AfertettuIcons.Folder, contentDescription = "Folders")
            }
        }

        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text("Search people or paste a link") },
            leadingIcon = { Icon(AfertettuIcons.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(AfertettuIcons.Close, contentDescription = "Clear")
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                when {
                    candidate != null && !alreadyFollowed -> open(candidate)
                    visible.size == 1 -> open(visible.first().handle)
                    else -> focus.clearFocus()
                }
            }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp + LocalReadableInset.current)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp + LocalReadableInset.current,
                top = 16.dp,
                end = 16.dp + LocalReadableInset.current,
                bottom = 16.dp + LocalDockPadding.current
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (candidate != null && !alreadyFollowed) {
                item(key = "candidate") {
                    CandidateCard(
                        handle = candidate,
                        onOpen = { open(candidate) },
                        onFollow = { viewModel.follow(candidate) }
                    )
                }
            } else if (trimmed.isNotEmpty() && candidate == null && visible.isEmpty() && people.isEmpty()) {
                item(key = "invalid") {
                    Hint("No one found yet. Search a name, or type a whole handle like jane.bsky.social.")
                }
            }

            items(visible, key = { it.handle }) { row ->
                AccountCard(
                    row = row,
                    onClick = { open(row.handle) },
                    // Only once the reader has made a folder. With Main alone
                    // the chip would name the one place everything is.
                    onFile = if (folders.size > 1) ({ filing = row }) else null
                )
            }

            // People from the search, those already followed left out: they
            // are in the list above.
            val newPeople = people.filterNot { actor -> rows.any { it.handle.equals(actor.handle, ignoreCase = true) } }
            if (newPeople.isNotEmpty()) {
                item(key = "people-title") {
                    Text(
                        "People on Bluesky",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                items(newPeople, key = { "person-${it.handle}" }) { actor ->
                    PersonCard(actor = actor, onOpen = { open(actor.handle) }, onFollow = { viewModel.follow(actor.handle) })
                }
            }

            if (rows.isEmpty() && trimmed.isEmpty()) {
                item(key = "empty") { EmptyState() }
            }
        }
    }
}

@Composable
private fun AccountCard(row: AccountRow, onClick: () -> Unit, onFile: (() -> Unit)?) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.zone,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Avatar(url = row.avatarUrl, name = row.name ?: row.handle, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    row.name ?: "@${row.handle}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (row.name != null) {
                    Text(
                        "@${row.handle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    row.lastPostMillis?.let(::relativeTime) ?: "Not read yet",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // The folder is a control, not a label: tapping the card opens
                // the account, tapping this files it.
                if (onFile != null) {
                    AssistChip(
                        onClick = onFile,
                        label = { Text(row.folder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(AfertettuIcons.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateCard(handle: String, onOpen: () -> Unit, onFollow: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Avatar(url = null, name = handle, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    "@$handle",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Tap to read the profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            BoldButton(onClick = onFollow, filled = true) { Text("Follow") }
        }
    }
}

@Composable
private fun PersonCard(actor: Actor, onOpen: () -> Unit, onFollow: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.zone,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Avatar(url = actor.avatarUrl, name = actor.displayName ?: actor.handle, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    actor.displayName?.takeIf { it.isNotBlank() } ?: "@${actor.handle}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "@${actor.handle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BoldButton(onClick = onFollow) { Text("Follow") }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(24.dp)
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            AfertettuIcons.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text("No accounts yet", style = MaterialTheme.typography.titleLarge)
        Text(
            "Search a name or paste a profile link above, then follow to build your timeline. " +
                "The list stays on this phone. Only the reads go to Bluesky.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
