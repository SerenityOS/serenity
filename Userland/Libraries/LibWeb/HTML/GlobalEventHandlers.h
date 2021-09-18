/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Forward.h>

#define ENUMERATE_GLOBAL_EVENT_HANDLERS(E)                    \
    E(onabort, HTML::EventNames::abort)                       \
    E(onauxclick, "auxclick")                                 \
    E(onblur, HTML::EventNames::blur)                         \
    E(oncancel, HTML::EventNames::cancel)                     \
    E(oncanplay, "canplay")                                   \
    E(oncanplaythrough, "canplaythrough")                     \
    E(onchange, HTML::EventNames::change)                     \
    E(onclick, UIEvents::EventNames::click)                   \
    E(onclose, HTML::EventNames::close)                       \
    E(oncontextmenu, HTML::EventNames::contextmenu)           \
    E(oncuechange, "cuechange")                               \
    E(ondblclick, "dblclick")                                 \
    E(ondrag, "drag")                                         \
    E(ondragend, "dragend")                                   \
    E(ondragenter, "dragenter")                               \
    E(ondragleave, "dragleave")                               \
    E(ondragover, "dragover")                                 \
    E(ondragstart, "dragstart")                               \
    E(ondrop, "drop")                                         \
    E(ondurationchange, "durationchange")                     \
    E(onemptied, "emptied")                                   \
    E(onended, "ended")                                       \
    E(onerror, HTML::EventNames::error)                       \
    E(onfocus, "focus")                                       \
    E(onformdata, "formdata")                                 \
    E(oninput, HTML::EventNames::input)                       \
    E(oninvalid, HTML::EventNames::invalid)                   \
    E(onkeydown, "keydown")                                   \
    E(onkeypress, "keypress")                                 \
    E(onkeyup, "keyup")                                       \
    E(onload, HTML::EventNames::load)                         \
    E(onloadeddata, "loadeddata")                             \
    E(onloadedmetadata, "loadedmetadata")                     \
    E(onloadstart, "loadstart")                               \
    E(onmousedown, UIEvents::EventNames::mousedown)           \
    E(onmouseenter, UIEvents::EventNames::mouseenter)         \
    E(onmouseleave, UIEvents::EventNames::mouseleave)         \
    E(onmousemove, UIEvents::EventNames::mousemove)           \
    E(onmouseout, UIEvents::EventNames::mouseout)             \
    E(onmouseover, UIEvents::EventNames::mouseover)           \
    E(onmouseup, UIEvents::EventNames::mouseup)               \
    E(onpause, "pause")                                       \
    E(onplay, "play")                                         \
    E(onplaying, "playing")                                   \
    E(onprogress, "progress")                                 \
    E(onratechange, "ratechange")                             \
    E(onreset, "reset")                                       \
    E(onresize, "resize")                                     \
    E(onscroll, "scroll")                                     \
    E(onsecuritypolicyviolation, "securitypolicyviolation")   \
    E(onseeked, "seeked")                                     \
    E(onseeking, "seeking")                                   \
    E(onselect, HTML::EventNames::select)                     \
    E(onslotchange, "slotchange")                             \
    E(onstalled, "stalled")                                   \
    E(onsubmit, HTML::EventNames::submit)                     \
    E(onsuspend, "suspend")                                   \
    E(ontimeupdate, "timeupdate")                             \
    E(ontoggle, "toggle")                                     \
    E(onvolumechange, "volumechange")                         \
    E(onwaiting, "waiting")                                   \
    E(onwebkitanimationend, "webkitanimationend")             \
    E(onwebkitanimationiteration, "webkitanimationiteration") \
    E(onwebkitanimationstart, "webkitanimationstart")         \
    E(onwebkittransitionend, "webkittransitionend")           \
    E(onwheel, "wheel")

namespace Web::HTML {

class GlobalEventHandlers {
public:
    virtual ~GlobalEventHandlers();

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)    \
    void set_##attribute_name(HTML::EventHandler); \
    HTML::EventHandler attribute_name();
    ENUMERATE_GLOBAL_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

protected:
    virtual DOM::EventTarget& global_event_handlers_to_event_target() = 0;
};

}
