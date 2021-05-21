/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibVT/XtermColors.h>

namespace VT {

struct Attribute {
    Attribute() { reset(); }

    static const u32 default_foreground_color = xterm_colors[7];
    static const u32 default_background_color = xterm_colors[0];

    void reset()
    {
        foreground_color = default_foreground_color;
        background_color = default_background_color;
        flags = Flags::NoAttributes;
    }
    u32 foreground_color {};
    u32 background_color {};

    u32 effective_background_color() const { return flags & Negative ? foreground_color : background_color; }
    u32 effective_foreground_color() const { return flags & Negative ? background_color : foreground_color; }

#ifndef KERNEL
    String href;
    String href_id;
#endif

    enum Flags : u8 {
        NoAttributes = 0x00,
        Bold = 0x01,
        Italic = 0x02,
        Underline = 0x04,
        Negative = 0x08,
        Blink = 0x10,
        Touched = 0x20,
    };

    bool is_untouched() const { return !(flags & Touched); }

    // TODO: it would be really nice if we had a helper for enums that
    // exposed bit ops for class enums...
    u8 flags = Flags::NoAttributes;

    bool operator==(const Attribute& other) const
    {
        return foreground_color == other.foreground_color && background_color == other.background_color && flags == other.flags;
    }
    bool operator!=(const Attribute& other) const
    {
        return !(*this == other);
    }
};

}
