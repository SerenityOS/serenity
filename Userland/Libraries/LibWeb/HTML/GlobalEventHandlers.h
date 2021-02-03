/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    E(onmousemove, UIEvents::EventNames::mousedown)           \
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

    void set_event_handler_attribute(const FlyString& name, HTML::EventHandler);
    HTML::EventHandler get_event_handler_attribute(const FlyString& name);

protected:
    virtual DOM::EventTarget& global_event_handlers_to_event_target() = 0;
};

}
