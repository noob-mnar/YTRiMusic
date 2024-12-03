package me.knighthat.component.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import it.fast4x.rimusic.ui.screens.settings.SettingsDescription
import it.fast4x.rimusic.utils.RestartPlayerService

abstract class SettingEntry(
    private val description: String,
    private val requirePlayerRestart: MutableState<Boolean>?,
    mutableShowRequestToRestart: MutableState<Boolean>
) {

    var showRequestToRestart by mutableShowRequestToRestart

    @Composable
    abstract fun Content()

    @Composable
    fun Draw() {
        LaunchedEffect( requirePlayerRestart?.value ) {
            /*
               Setting [showRequestToRestart] to false when
               global [requirePlayerRestart] is false to prevent
               prompt from coming up again when other settings
               request restart on player service.
             */
            if( requirePlayerRestart?.value == false )
                showRequestToRestart = false
        }

        Content()

        if( description.isNotBlank() )
            SettingsDescription( description )

        RestartPlayerService(
            requirePlayerRestart?.value == true && showRequestToRestart
        ) {
            requirePlayerRestart?.value = false
            showRequestToRestart = false
        }
    }
}