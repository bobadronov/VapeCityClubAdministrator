package org.bigblackowl.vccadmin.data.entity

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.transition_fade
import vccadministrator.composeapp.generated.resources.transition_pop
import vccadministrator.composeapp.generated.resources.transition_slide_fade
import vccadministrator.composeapp.generated.resources.transition_slide_horizontal
import vccadministrator.composeapp.generated.resources.transition_slide_vertical
import vccadministrator.composeapp.generated.resources.transition_zoom

@Serializable
enum class TransitionEffect {

    FADE,
    SLIDE_HORIZONTAL,
    SLIDE_VERTICAL,
    ZOOM,
    SLIDE_FADE,
    POP;

    val labelRes: StringResource
        get() = when (this) {
            FADE -> Res.string.transition_fade
            SLIDE_HORIZONTAL -> Res.string.transition_slide_horizontal
            SLIDE_VERTICAL -> Res.string.transition_slide_vertical
            ZOOM -> Res.string.transition_zoom
            SLIDE_FADE -> Res.string.transition_slide_fade
            POP -> Res.string.transition_pop
        }

}