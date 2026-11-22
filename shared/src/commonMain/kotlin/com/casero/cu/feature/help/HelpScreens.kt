package com.casero.cu.feature.help

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.casero.cu.network.CaseroClient
import com.casero.cu.reporting.SmsReporter
import com.casero.cu.resources.Res
import com.casero.cu.resources.app_user_manual
import com.casero.cu.resources.cubatramite_source
import com.casero.cu.resources.guide_cubatramite_is_an_independent_site_and_this_is
import com.casero.cu.resources.guide_notes
import com.casero.cu.resources.guide_offline_summary_of_the_cubatramite_guide_reporte
import com.casero.cu.resources.guide_open_the_full_guide
import com.casero.cu.resources.help_notes
import com.casero.cu.resources.help_open
import com.casero.cu.resources.help_open_help_in_the_official_portal
import com.casero.cu.resources.help_these_instructions_come_from_the_official_casero
import com.casero.cu.resources.manual_casero_helps_you_report_your_guests_to
import com.casero.cu.resources.manual_notes
import com.casero.cu.resources.official_portal_help
import com.casero.cu.ui.BackButton
import com.casero.cu.ui.Caption
import com.casero.cu.ui.NumberedStep
import com.casero.cu.ui.SecondaryButton
import com.casero.cu.ui.card
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** One numbered instruction, with optional indented follow-ups. */
data class HelpStep(val text: StringResource, val details: List<StringResource> = emptyList())

/** Building blocks for long-form help screens. */
sealed interface HelpBlock {
    data class Paragraph(val text: StringResource) : HelpBlock
    data class Heading(val text: StringResource) : HelpBlock
    data class Terms(val terms: List<Pair<StringResource, StringResource>>) : HelpBlock
    data class Steps(val steps: List<HelpStep>) : HelpBlock
    data class Bullets(val items: List<StringResource>) : HelpBlock
    data class Notes(val notes: List<StringResource>) : HelpBlock

    /** Literal text such as SMS examples; shown monospaced and never translated. */
    data class Example(val literal: String) : HelpBlock
}

/** A titled group of help blocks. Portal help topics also carry their portal page and tile. */
data class HelpSection(
    val id: String,
    val title: StringResource,
    val icon: ImageVector,
    val blocks: List<HelpBlock>,
    val color: Color? = null,
    val path: String? = null,
    val image: DrawableResource? = null,
)

/** Renders help blocks inside a card. */
@Composable
fun HelpBlocksCard(blocks: List<HelpBlock>, notesTitle: StringResource) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.card, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        blocks.forEachIndexed { index, block ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            when (block) {
                is HelpBlock.Paragraph -> Text(stringResource(block.text), style = MaterialTheme.typography.bodyMedium)
                is HelpBlock.Heading -> Text(stringResource(block.text), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                is HelpBlock.Terms -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((term, detail) in block.terms) {
                        val termText = stringResource(term)
                        val detailText = stringResource(detail)
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(termText) }
                                append(": ")
                                append(detailText)
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is HelpBlock.Steps -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    block.steps.forEachIndexed { stepIndex, step ->
                        NumberedStep(stepIndex + 1, null, stringResource(step.text), step.details.map { stringResource(it) })
                    }
                }
                is HelpBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (item in block.items) {
                        Row {
                            Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(item), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is HelpBlock.Notes -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(notesTitle), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    for (note in block.notes) {
                        Text(stringResource(note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is HelpBlock.Example -> SelectionContainer {
                    Text(
                        block.literal,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { BackButton(onBack) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content() }
    }
}

@Composable
private fun SectionTitle(section: HelpSection) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(section.title), style = MaterialTheme.typography.titleMedium)
    }
}

/** Everything the app can do, tab by tab. */
@Composable
fun UserManualScreen(onBack: () -> Unit) {
    HelpScaffold(stringResource(Res.string.app_user_manual), onBack) {
        Caption(stringResource(Res.string.manual_casero_helps_you_report_your_guests_to))
        for (section in userManualSections) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(section)
                HelpBlocksCard(section.blocks, Res.string.manual_notes)
            }
        }
    }
}

/**
 * Offline summary of the third-party Cubatramite guide on reporting guests to MININT.
 * Written in our own words from the guide dated October 29, 2026; the full article stays one tap away.
 */
@Composable
fun CubatramiteGuideScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    HelpScaffold(stringResource(Res.string.cubatramite_source), onBack) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.guide_offline_summary_of_the_cubatramite_guide_reporte), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(Res.string.guide_cubatramite_is_an_independent_site_and_this_is),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SecondaryButton(stringResource(Res.string.guide_open_the_full_guide), { uriHandler.openUri(SmsReporter.INSTRUCTIONS_URL) }, icon = Icons.Default.Public)
        for (section in cubatramiteGuideSections) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(section)
                HelpBlocksCard(section.blocks, Res.string.guide_notes)
            }
        }
    }
}

/** Localized copy of the official portal's own help, with deep links to each portal page. */
@Composable
fun PortalHelpScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    HelpScaffold(stringResource(Res.string.official_portal_help), onBack) {
        Caption(stringResource(Res.string.help_these_instructions_come_from_the_official_casero))
        SecondaryButton(
            stringResource(Res.string.help_open_help_in_the_official_portal),
            { uriHandler.openUri(CaseroClient.portalUrl("Principal/Ayuda")) },
            icon = Icons.Default.Public,
        )
        for (topic in portalHelpTopics) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PortalTile(topic)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(topic.title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { uriHandler.openUri(CaseroClient.portalUrl(topic.path ?: "")) }) {
                        Icon(Icons.Default.Public, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.help_open))
                    }
                }
                HelpBlocksCard(topic.blocks, Res.string.help_notes)
            }
        }
    }
}

/** Tile copied from the portal's home page, so users recognize the section on the website. */
@Composable
private fun PortalTile(topic: HelpSection) {
    val shape = RoundedCornerShape(3.dp)
    val image = topic.image
    if (image != null) {
        Image(painterResource(image), contentDescription = null, Modifier.size(32.dp).clip(shape))
    } else {
        Box(Modifier.size(32.dp).clip(shape).background(topic.color ?: MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(topic.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}
