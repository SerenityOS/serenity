/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/UIEvents/EventNames.h>

namespace Web::UIEvents::EventNames {

#define __ENUMERATE_UI_EVENT(name) DeprecatedFlyString name;
ENUMERATE_UI_EVENTS
#undef __ENUMERATE_UI_EVENT

ErrorOr<void> initialize_strings()
{
    static bool s_initialized = false;
    VERIFY(!s_initialized);

#define __ENUMERATE_UI_EVENT(name) \
    name = #name;
    ENUMERATE_UI_EVENTS
#undef __ENUMERATE_UI_EVENT

    s_initialized = true;
    return {};
}

}
