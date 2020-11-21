/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/FlyString.h>

namespace Web::HTML::EventNames {

// FIXME: Add media events https://html.spec.whatwg.org/multipage/media.html#mediaevents
// FIXME: Add app cache events https://html.spec.whatwg.org/multipage/offline.html#appcacheevents
// FIXME: Add drag and drop events https://html.spec.whatwg.org/multipage/dnd.html#dndevents

#define ENUMERATE_HTML_EVENTS                       \
    __ENUMERATE_HTML_EVENT(abort)                   \
    __ENUMERATE_HTML_EVENT(DOMContentLoaded)        \
    __ENUMERATE_HTML_EVENT(afterprint)              \
    __ENUMERATE_HTML_EVENT(beforeprint)             \
    __ENUMERATE_HTML_EVENT(beforeunload)            \
    __ENUMERATE_HTML_EVENT(blur)                    \
    __ENUMERATE_HTML_EVENT(cancel)                  \
    __ENUMERATE_HTML_EVENT(change)                  \
    __ENUMERATE_HTML_EVENT(click)                   \
    __ENUMERATE_HTML_EVENT(close)                   \
    __ENUMERATE_HTML_EVENT(connect)                 \
    __ENUMERATE_HTML_EVENT(contextmenu)             \
    __ENUMERATE_HTML_EVENT(copy)                    \
    __ENUMERATE_HTML_EVENT(cut)                     \
    __ENUMERATE_HTML_EVENT(error)                   \
    __ENUMERATE_HTML_EVENT(focus)                   \
    __ENUMERATE_HTML_EVENT(formdata)                \
    __ENUMERATE_HTML_EVENT(hashchange)              \
    __ENUMERATE_HTML_EVENT(input)                   \
    __ENUMERATE_HTML_EVENT(invalid)                 \
    __ENUMERATE_HTML_EVENT(languagechange)          \
    __ENUMERATE_HTML_EVENT(load)                    \
    __ENUMERATE_HTML_EVENT(message)                 \
    __ENUMERATE_HTML_EVENT(messageerror)            \
    __ENUMERATE_HTML_EVENT(offline)                 \
    __ENUMERATE_HTML_EVENT(online)                  \
    __ENUMERATE_HTML_EVENT(open)                    \
    __ENUMERATE_HTML_EVENT(pagehide)                \
    __ENUMERATE_HTML_EVENT(pageshow)                \
    __ENUMERATE_HTML_EVENT(paste)                   \
    __ENUMERATE_HTML_EVENT(popstate)                \
    __ENUMERATE_HTML_EVENT(readystatechange)        \
    __ENUMERATE_HTML_EVENT(rejectionhandled)        \
    __ENUMERATE_HTML_EVENT(reset)                   \
    __ENUMERATE_HTML_EVENT(securitypolicyviolation) \
    __ENUMERATE_HTML_EVENT(select)                  \
    __ENUMERATE_HTML_EVENT(slotchange)              \
    __ENUMERATE_HTML_EVENT(storage)                 \
    __ENUMERATE_HTML_EVENT(submit)                  \
    __ENUMERATE_HTML_EVENT(toggle)                  \
    __ENUMERATE_HTML_EVENT(unhandledrejection)      \
    __ENUMERATE_HTML_EVENT(unload)

#define __ENUMERATE_HTML_EVENT(name) extern FlyString name;
ENUMERATE_HTML_EVENTS
#undef __ENUMERATE_HTML_EVENT

}
