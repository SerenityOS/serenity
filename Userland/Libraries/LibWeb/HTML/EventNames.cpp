/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/EventNames.h>

namespace Web::HTML::EventNames {

#define __ENUMERATE_HTML_EVENT(name) FlyString name;
ENUMERATE_HTML_EVENTS
#undef __ENUMERATE_HTML_EVENT

[[gnu::constructor]] static void initialize()
{
    static bool s_initialized = false;
    if (s_initialized)
        return;

#define __ENUMERATE_HTML_EVENT(name) \
    name = #name;
    ENUMERATE_HTML_EVENTS
#undef __ENUMERATE_HTML_EVENT

    s_initialized = true;
}

}
