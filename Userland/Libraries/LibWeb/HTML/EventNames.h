/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    __ENUMERATE_HTML_EVENT(scroll)                  \
    __ENUMERATE_HTML_EVENT(securitypolicyviolation) \
    __ENUMERATE_HTML_EVENT(select)                  \
    __ENUMERATE_HTML_EVENT(slotchange)              \
    __ENUMERATE_HTML_EVENT(storage)                 \
    __ENUMERATE_HTML_EVENT(submit)                  \
    __ENUMERATE_HTML_EVENT(toggle)                  \
    __ENUMERATE_HTML_EVENT(unhandledrejection)      \
    __ENUMERATE_HTML_EVENT(unload)                  \
    __ENUMERATE_HTML_EVENT(visibilitychange)

#define __ENUMERATE_HTML_EVENT(name) extern FlyString name;
ENUMERATE_HTML_EVENTS
#undef __ENUMERATE_HTML_EVENT

}
