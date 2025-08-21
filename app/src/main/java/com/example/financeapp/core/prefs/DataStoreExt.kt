package com.example.financeapp.core.prefs

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// Single app-wide DataStore named "settings"
val Context.dataStore by preferencesDataStore(name = "settings")
