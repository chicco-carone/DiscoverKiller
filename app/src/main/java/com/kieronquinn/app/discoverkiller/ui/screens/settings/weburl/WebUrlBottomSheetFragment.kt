package com.kieronquinn.app.discoverkiller.ui.screens.settings.weburl

import android.text.InputType
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.bottomsheet.BaseRssConfigureBottomSheet
import org.koin.androidx.viewmodel.ext.android.viewModel

class WebUrlBottomSheetFragment: BaseRssConfigureBottomSheet() {

    override val viewModel by viewModel<WebUrlBottomSheetViewModel>()

    override val title = R.string.settings_web_url
    override val hint = R.string.settings_web_url_hint
    override val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI

}
