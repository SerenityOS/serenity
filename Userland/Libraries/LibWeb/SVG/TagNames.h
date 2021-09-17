/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Web::SVG::TagNames {

#define ENUMERATE_SVG_GRAPHICS_TAGS \
    __ENUMERATE_SVG_TAG(g)          \
    __ENUMERATE_SVG_TAG(path)       \
    __ENUMERATE_SVG_TAG(svg)

#define ENUMERATE_SVG_TAGS             \
    ENUMERATE_SVG_GRAPHICS_TAGS        \
    __ENUMERATE_SVG_TAG(desc)          \
    __ENUMERATE_SVG_TAG(foreignObject) \
    __ENUMERATE_SVG_TAG(script)        \
    __ENUMERATE_SVG_TAG(title)

#define __ENUMERATE_SVG_TAG(name) extern FlyString name;
ENUMERATE_SVG_TAGS
#undef __ENUMERATE_SVG_TAG

}
