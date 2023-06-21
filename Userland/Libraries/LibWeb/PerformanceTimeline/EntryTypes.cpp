/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/PerformanceTimeline/EntryTypes.h>

namespace Web::PerformanceTimeline::EntryTypes {

#define __ENUMERATE_PERFORMANCE_TIMELINE_ENTRY_TYPE(name) FlyString name;
ENUMERATE_PERFORMANCE_TIMELINE_ENTRY_TYPES
#undef __ENUMERATE_PERFORMANCE_TIMELINE_ENTRY_TYPE

ErrorOr<void> initialize_strings()
{
    static bool s_initialized = false;
    VERIFY(!s_initialized);

#define __ENUMERATE_PERFORMANCE_TIMELINE_ENTRY_TYPE(name) \
    name = TRY(#name##_fly_string);
    ENUMERATE_PERFORMANCE_TIMELINE_ENTRY_TYPES
#undef __ENUMERATE_PERFORMANCE_TIMELINE_ENTRY_TYPE

    // NOTE: Special cases for attributes with dashes in them.
    first_input = TRY("first-input"_fly_string);
    largest_contentful_paint = TRY("largest-contentful-paint"_fly_string);
    layout_shift = TRY("layout-shift"_fly_string);

    s_initialized = true;
    return {};
}

}
