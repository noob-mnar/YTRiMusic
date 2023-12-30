package it.fast4x.rimusic.ui.screens.settings

//import it.fast4x.rimusic.BuildConfig
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.BuildConfig
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.ui.styling.onOverlay
import it.fast4x.rimusic.ui.styling.shimmer
import it.fast4x.rimusic.utils.isAvailableUpdate
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.secondary
import it.fast4x.rimusic.utils.thumbnailRoundnessKey


@ExperimentalAnimationApi
@Composable
fun About() {
    val (colorPalette, typography) = LocalAppearance.current
    val uriHandler = LocalUriHandler.current

    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    val newVersion = isAvailableUpdate()
    //val newVersion = ""

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        HeaderWithIcon(
            title = stringResource(R.string.about),
            iconId = R.drawable.information,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
        ) {
            BasicText(
                text = "RiMusic v${BuildConfig.VERSION_NAME} by fast4x",
                style = typography.s.secondary,

                )
        }

        if (newVersion != "") {
            //SettingsEntryGroupText(title = "Update available")
            SettingsEntry(
                title = "New version $newVersion",
                text = "Click here to open page",
                onClick = {
                    uriHandler.openUri("https://github.com/fast4x/RiMusic/releases/latest")
                    //uriHandler.openUri("https://github.com/fast4x/RiMusic/releases/tag/v0.6.9")
                },
                trailingContent = {
                    Image(
                        painter = painterResource(R.drawable.direct_download),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.shimmer),
                        modifier = Modifier
                            .size(34.dp)
                    )
                },
                modifier = Modifier
                    .background(
                        color = colorPalette.background4,
                        shape = thumbnailRoundness.shape()
                    )

            )
        }

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.social))

        SettingsEntry(
            title = "GitHub",
            text = stringResource(R.string.view_the_source_code),
            onClick = {
                uriHandler.openUri("https://github.com/fast4x/RiMusic")
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.troubleshooting))

        SettingsEntry(
            title = stringResource(R.string.report_an_issue),
            text = stringResource(R.string.you_will_be_redirected_to_github),
            onClick = {
                uriHandler.openUri("https://github.com/fast4x/RiMusic/issues/new?assignees=&labels=bug&template=bug_report.yaml")
            }
        )


        SettingsEntry(
            title = stringResource(R.string.request_a_feature_or_suggest_an_idea),
            text = stringResource(R.string.you_will_be_redirected_to_github),
            onClick = {
                uriHandler.openUri("https://github.com/fast4x/RiMusic/issues/new?assignees=&labels=feature_request&template=feature_request.yaml")
            }
        )

        SettingsGroupSpacer()

        SettingsEntryGroupText(title = stringResource(R.string.contributors))
        SettingsDescription(text = stringResource(R.string.in_alphabetical_order))

        SettingsTopDescription( text =
                "2010furs \n" +
                "25huizengek1 \n" +
                "abfreeman \n" +
                "Bash.boy \n" +
                "DanielSevillano \n" +
                "dainvincible1 \n" +
                "damianadam000 \n" +
                "Get100percent \n" +
                "ikanakova \n" +
                "mapkaps \n" +
                "NEVARLeVrai \n" +
                "OrangeZXZ \n" +
                "roklc \n" +
                "r.t.redreovic \n" +
                "Bash.boy \n" +
                "siggi1984 \n" +
                "teddysulaimanGL \n"
        )
    }
}
