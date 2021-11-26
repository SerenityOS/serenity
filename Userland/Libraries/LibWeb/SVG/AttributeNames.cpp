/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/SVG/AttributeNames.h>

namespace Web::SVG::AttributeNames {

#define __ENUMERATE_SVG_ATTRIBUTE(name) FlyString name;
ENUMERATE_SVG_ATTRIBUTES(__ENUMERATE_SVG_ATTRIBUTE)
#undef __ENUMERATE_SVG_ATTRIBUTE

[[gnu::constructor]] static void initialize()
{
    static bool s_initialized = false;
    if (s_initialized)
        return;

#define __ENUMERATE_SVG_ATTRIBUTE(name) \
    name = #name;
    ENUMERATE_SVG_ATTRIBUTES(__ENUMERATE_SVG_ATTRIBUTE)
#undef __ENUMERATE_SVG_ATTRIBUTE

    s_initialized = true;
}

}
