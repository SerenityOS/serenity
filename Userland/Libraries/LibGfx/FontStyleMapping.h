/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace Gfx {

struct FontStyleMapping {
    constexpr FontStyleMapping(int s, const char* n)
        : style(s)
        , name(n)
    {
    }
    int style { 0 };
    StringView name;
};

static constexpr FontStyleMapping font_weight_names[] = {
    { 100, "Thin" },
    { 200, "Extra Light" },
    { 300, "Light" },
    { 400, "Regular" },
    { 500, "Medium" },
    { 600, "Semi Bold" },
    { 700, "Bold" },
    { 800, "Extra Bold" },
    { 900, "Black" },
    { 950, "Extra Black" },
};

static constexpr FontStyleMapping font_slope_names[] = {
    { 0, "Regular" },
    { 1, "Italic" },
    { 2, "Oblique" },
    { 3, "Reclined" }
};

static constexpr StringView weight_to_name(int weight)
{
    for (auto& it : font_weight_names) {
        if (it.style == weight)
            return it.name;
    }
    return {};
}

static constexpr int name_to_weight(StringView name)
{
    for (auto& it : font_weight_names) {
        if (it.name == name)
            return it.style;
    }
    return {};
}

static constexpr StringView slope_to_name(int slope)
{
    for (auto& it : font_slope_names) {
        if (it.style == slope)
            return it.name;
    }
    return {};
}

static constexpr int name_to_slope(StringView name)
{
    for (auto& it : font_slope_names) {
        if (it.name == name)
            return it.style;
    }
    return {};
}

}
