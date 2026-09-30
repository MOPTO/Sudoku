package com.ariadna.sudoku.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    val settings: Flow<UserSettings>
}

/** Always the defaults. Replaced by the DataStore-backed repository in milestone 7. */
@Singleton
class DefaultSettingsRepository @Inject constructor() : SettingsRepository {
    override val settings: Flow<UserSettings> = MutableStateFlow(UserSettings())
}
