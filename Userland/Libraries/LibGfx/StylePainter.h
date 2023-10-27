/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/TabPosition.h>

namespace Gfx {

enum class ButtonStyle {
    Normal,
    ThickCap,
    Coolbar,
    Tray,
};

enum class FrameStyle {
    NoFrame,
    Window,
    Plain,
    RaisedBox,
    SunkenBox,
    RaisedContainer,
    SunkenContainer,
    RaisedPanel,
    SunkenPanel,
};

// FIXME: should this be in its own header?
class BaseStylePainter {
public:
    virtual ~BaseStylePainter() = default;

    virtual void paint_button(Painter&, IntRect const&, Palette const&, ButtonStyle, bool pressed, bool hovered = false, bool checked = false, bool enabled = true, bool focused = false, bool default_button = false) = 0;
    virtual void paint_tab_button(Painter&, IntRect const&, Palette const&, bool active, bool hovered, bool enabled, TabPosition position, bool in_active_window, bool accented) = 0;
    virtual void paint_frame(Painter&, IntRect const&, Palette const&, FrameStyle, bool skip_vertical_lines = false) = 0;
    virtual void paint_window_frame(Painter&, IntRect const&, Palette const&) = 0;
    virtual void paint_progressbar(Painter&, IntRect const&, Palette const&, int min, int max, int value, StringView text, Orientation = Orientation::Horizontal) = 0;
    virtual void paint_radio_button(Painter&, IntRect const&, Palette const&, bool is_checked, bool is_being_pressed) = 0;
    virtual void paint_check_box(Painter&, IntRect const&, Palette const&, bool is_enabled, bool is_checked, bool is_being_pressed) = 0;
    virtual void paint_transparency_grid(Painter&, IntRect const&, Palette const&) = 0;
    virtual void paint_simple_rect_shadow(Painter&, IntRect const&, Bitmap const& shadow_bitmap, bool shadow_includes_frame = false, bool fill_content = false) = 0;

protected:
    BaseStylePainter() = default;
};

class StylePainter {
public:
    static BaseStylePainter& current();

    // FIXME: These are here for API compatibility, we should probably remove them and move BaseStylePainter into here
    static void paint_button(Painter&, IntRect const&, Palette const&, ButtonStyle, bool pressed, bool hovered = false, bool checked = false, bool enabled = true, bool focused = false, bool default_button = false);
    static void paint_tab_button(Painter&, IntRect const&, Palette const&, bool active, bool hovered, bool enabled, TabPosition position, bool in_active_window, bool accented);
    static void paint_frame(Painter&, IntRect const&, Palette const&, FrameStyle, bool skip_vertical_lines = false);
    static void paint_window_frame(Painter&, IntRect const&, Palette const&);
    static void paint_progressbar(Painter&, IntRect const&, Palette const&, int min, int max, int value, StringView text, Orientation = Orientation::Horizontal);
    static void paint_radio_button(Painter&, IntRect const&, Palette const&, bool is_checked, bool is_being_pressed);
    static void paint_check_box(Painter&, IntRect const&, Palette const&, bool is_enabled, bool is_checked, bool is_being_pressed);
    static void paint_transparency_grid(Painter&, IntRect const&, Palette const&);
    static void paint_simple_rect_shadow(Painter&, IntRect const&, Bitmap const& shadow_bitmap, bool shadow_includes_frame = false, bool fill_content = false);
};

}
