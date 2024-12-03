package me.knighthat.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import it.fast4x.rimusic.ui.screens.settings.EnumValueSelectorSettingsEntry
import it.fast4x.rimusic.ui.screens.settings.SwitchSettingEntry
import me.knighthat.component.Search
import me.knighthat.component.screen.settings.SettingEntry

@Composable
fun ConditionalSettingEntry(
    isVisible: Boolean,
    content: @Composable () -> Unit
) { if (isVisible) content() }

@Composable
fun SearchableSettingEntry(
    searchComp: Search,
    title: String,
    content: @Composable () -> Unit
) {
    if( searchComp.input.isBlank() || title.contains( searchComp.input, true ) )
        content()
}


/*
 *
 *      DEFAULT
 *
 */

@Composable
inline fun <reified T: Enum<T>> EnumSettingEntry(
    mutableValue: MutableState<T>,
    title: String,
    description: String = "",
    isEnabled: Boolean = true,
    requirePlayerRestart: MutableState<Boolean>? = null,
    crossinline onValueSelected: (T) -> Unit = {},
    noinline valueText: @Composable (T) -> String = { "" },
    children: @Composable (T) -> Unit = {}
) {
    var selected by mutableValue
    val mutableShowRequestToRestart = rememberSaveable { mutableStateOf( false ) }

    object : SettingEntry(
        description,
        requirePlayerRestart,
        mutableShowRequestToRestart
    ) {
        @Composable
        override fun Content() {

            EnumValueSelectorSettingsEntry(
                title = title,
                selectedValue = selected,
                isEnabled = isEnabled,
                onValueSelected = {
                    selected = it
                    onValueSelected( it )

                    if( requirePlayerRestart != null ) {
                        requirePlayerRestart.value = true
                        showRequestToRestart = true
                    }
                },
                valueText = valueText
            )

        }
    }.Draw()

    children( selected )
}

@Composable
fun ToggleSettingEntry(
    mutableValue: MutableState<Boolean>,
    title: String,
    description: String = "",
    isEnabled: Boolean = true,
    requirePlayerRestart: MutableState<Boolean>? = null,
    onStateChanged: (Boolean) -> Unit = {},
    children: @Composable (Boolean) -> Unit = {}
) {
    var state by mutableValue
    val mutableShowRequestToRestart = rememberSaveable { mutableStateOf( false ) }

    object: SettingEntry(
        description,
        requirePlayerRestart,
        mutableShowRequestToRestart
    ) {
        @Composable
        override fun Content() {
            SwitchSettingEntry(
                title = title,
                text = description,
                isChecked = state,
                onCheckedChange = {
                    state = it
                    onStateChanged( it )

                    if( requirePlayerRestart != null ) {
                        requirePlayerRestart.value = true
                        showRequestToRestart = true
                    }
                },
                isEnabled = isEnabled
            )

        }
    }.Draw()

    children( state )
}


/*
 *
 *      VARIANT
 *
 */

@Composable
inline fun <reified T: Enum<T>> EnumSettingEntry(
    mutableValue: MutableState<T>,
    searchComp: Search,
    title: String,
    description: String = "",
    isEnabled: Boolean = true,
    requirePlayerRestart: MutableState<Boolean>? = null,
    crossinline onValueSelected: (T) -> Unit = {},
    noinline valueText: @Composable (T) -> String = { "" },
    crossinline children: @Composable (T) -> Unit = {}
) {
    SearchableSettingEntry( searchComp, title ) {
        EnumSettingEntry(
            mutableValue,
            title,
            description,
            isEnabled,
            requirePlayerRestart,
            onValueSelected,
            valueText,
            children
        )
    }
}

@Composable
fun ToggleSettingEntry(
    mutableValue: MutableState<Boolean>,
    searchComp: Search,
    title: String,
    description: String = "",
    isEnabled: Boolean = true,
    requirePlayerRestart: MutableState<Boolean>? = null,
    onStateChanged: (Boolean) -> Unit = {},
    children: @Composable (Boolean) -> Unit = {}
) {
    SearchableSettingEntry( searchComp, title ) {
        ToggleSettingEntry(
            mutableValue,
            title,
            description,
            isEnabled,
            requirePlayerRestart,
            onStateChanged,
            children
        )
    }
}