package com.kieronquinn.app.discoverkiller.model.settings

import android.content.ComponentName
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository

data class OverlaySettings(
    @SerializedName("enabled")
    val enabled: Boolean,
    @SerializedName("overlay_type")
    val overlayType: SettingsRepository.OverlayType,
    @SerializedName("overlay_component")
    val overlayComponent: String,
    @SerializedName("original_handles_search")
    val originalHandlesSearch: Boolean,
    @SerializedName("entertainment_space_restart")
    val entertainmentSpaceRestart: Boolean,
    @SerializedName("overlay_mode")
    val overlayMode: SettingsRepository.OverlayMode,
    @SerializedName("overlay_app")
    val overlayApp: String,
    @SerializedName("overlay_app_new_task")
    val overlayAppNewTask: Boolean,
    @SerializedName("web_url")
    val webUrl: String
) {

    companion object {
        fun fromJson(json: String): OverlaySettings {
            return Gson().fromJson(json, OverlaySettings::class.java)
        }
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }

    fun getOverlayComponent(): ComponentName {
        return ComponentName.unflattenFromString(overlayComponent)!!
    }

}
