package com.kieronquinn.app.discoverkiller.ui.screens.settings.weburl

import com.kieronquinn.app.discoverkiller.components.navigation.ContainerNavigation
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.bottomsheet.BaseRssConfigureBottomSheetViewModelImpl

class WebUrlBottomSheetViewModel(
    navigation: ContainerNavigation,
    settings: SettingsRepository
): BaseRssConfigureBottomSheetViewModelImpl(navigation) {

    override val setting = settings.webUrl

}
