package com.quran.labs.androidquran.ui.compose

import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons

/** The frequently asked questions, with the support email address as a tappable link. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val linkColor = MaterialTheme.colorScheme.primary
  val helpHtml = stringResource(R.string.help)
  val emailText = stringResource(R.string.email_us)
  val body = remember(helpHtml) { AnnotatedString.fromHtml(helpHtml) }
  val email = remember(emailText, linkColor) { linkEmails(emailText, linkColor) }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.menu_help)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(QuranIcons.ArrowBack, contentDescription = stringResource(R.string.menu_back_to_page))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        scrollBehavior = scrollBehavior
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      Text(
        text = stringResource(R.string.help_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
      )
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(text = body, style = MaterialTheme.typography.bodyLarge)
          Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp)
          )
        }
      }
    }
  }
}

/** Makes every email address in [text] a link that opens the mail app. */
private fun linkEmails(text: String, color: androidx.compose.ui.graphics.Color): AnnotatedString {
  val styles = TextLinkStyles(SpanStyle(color = color, textDecoration = TextDecoration.Underline))
  return buildAnnotatedString {
    var last = 0
    val matcher = Patterns.EMAIL_ADDRESS.matcher(text)
    while (matcher.find()) {
      append(text.substring(last, matcher.start()))
      val address = matcher.group()
      withLink(LinkAnnotation.Url("mailto:$address", styles)) { append(address) }
      last = matcher.end()
    }
    append(text.substring(last))
  }
}
