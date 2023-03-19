/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/GrayscaleBitmap.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/CheckBox.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Painting/CheckBoxPaintable.h>

namespace Web::Painting {

// A 16x16 signed distance field for the checkbox's tick (slightly rounded):
static Array<u8, 16 * 16> s_check_mark_sdf {
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 251, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 231, 194, 189, 218, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 245, 193, 142, 131, 165, 205, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 209, 156, 105, 78, 116, 174, 237,
    254, 254, 254, 254, 254, 254, 254, 254, 226, 173, 120, 69, 79, 132, 185, 243,
    254, 254, 254, 254, 254, 254, 254, 243, 190, 138, 85, 62, 115, 167, 219, 254,
    254, 254, 227, 203, 212, 249, 254, 207, 154, 102, 50, 98, 149, 202, 254, 254,
    254, 225, 180, 141, 159, 204, 224, 171, 119, 67, 81, 134, 186, 238, 254, 254,
    243, 184, 135, 90, 113, 157, 188, 136, 84, 64, 116, 169, 221, 254, 254, 254,
    237, 174, 118, 71, 68, 113, 153, 100, 48, 100, 152, 204, 254, 254, 254, 254,
    254, 208, 162, 116, 71, 67, 107, 65, 83, 135, 187, 240, 254, 254, 254, 254,
    254, 251, 206, 162, 116, 71, 43, 66, 119, 171, 223, 254, 254, 254, 254, 254,
    254, 254, 251, 206, 162, 116, 73, 102, 154, 207, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 251, 206, 162, 124, 139, 190, 242, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 254, 251, 210, 187, 194, 229, 254, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 251, 254, 254, 254, 254, 254, 254, 254, 254, 254
};

// A 16x16 signed distance field for an indeterminate checkbox (rounded line)
// Note: We could use the AA fill_rect_with_rounded_corners() for this in future,
// though right now it can't draw at subpixel accuracy (so is misaligned and jitters when scaling).
static Array<u8, 16 * 16> s_check_indeterminate {
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 239, 211, 209, 209, 209, 209, 209, 209, 211, 237, 254, 254, 254,
    254, 254, 252, 195, 151, 145, 145, 145, 145, 145, 145, 150, 193, 250, 254, 254,
    254, 254, 243, 179, 115, 81, 81, 81, 81, 81, 81, 113, 177, 241, 254, 254,
    254, 254, 243, 179, 115, 79, 79, 79, 79, 79, 79, 113, 177, 241, 254, 254,
    254, 254, 251, 194, 149, 143, 143, 143, 143, 143, 143, 148, 192, 250, 254, 254,
    254, 254, 254, 237, 210, 207, 207, 207, 207, 207, 207, 209, 236, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254,
    254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254, 254
};

static constexpr Gfx::GrayscaleBitmap check_mark_sdf()
{
    return Gfx::GrayscaleBitmap(s_check_mark_sdf, 16, 16);
}

static constexpr Gfx::GrayscaleBitmap check_indeterminate_sdf()
{
    return Gfx::GrayscaleBitmap(s_check_indeterminate, 16, 16);
}

JS::NonnullGCPtr<CheckBoxPaintable>
CheckBoxPaintable::create(Layout::CheckBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<CheckBoxPaintable>(layout_box);
}

CheckBoxPaintable::CheckBoxPaintable(Layout::CheckBox const& layout_box)
    : LabelablePaintable(layout_box)
{
}

Layout::CheckBox const& CheckBoxPaintable::layout_box() const
{
    return static_cast<Layout::CheckBox const&>(layout_node());
}

Layout::CheckBox& CheckBoxPaintable::layout_box()
{
    return static_cast<Layout::CheckBox&>(layout_node());
}

void CheckBoxPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    PaintableBox::paint(context, phase);

    if (phase != PaintPhase::Foreground)
        return;

    auto const& checkbox = static_cast<HTML::HTMLInputElement const&>(layout_box().dom_node());
    bool enabled = layout_box().dom_node().enabled();
    Gfx::AntiAliasingPainter painter { context.painter() };
    auto checkbox_rect = context.enclosing_device_rect(absolute_rect()).to_type<int>();
    auto checkbox_radius = checkbox_rect.width() / 5;

    auto& palette = context.palette();

    auto shade = [&](Color color, float amount) {
        return color.mixed_with(palette.is_dark() ? Color::Black : Color::White, amount);
    };

    auto modify_color = [&](Color color) {
        if (being_pressed() && enabled)
            return shade(color, 0.3f);
        return color;
    };

    auto base_text_color = palette.color(ColorRole::BaseText);
    auto accent = computed_values().accent_color().value_or(palette.color(ColorRole::Accent));
    auto base = shade(base_text_color.inverted(), 0.8f);
    auto dark_gray = shade(base_text_color, 0.3f);
    auto gray = shade(dark_gray, 0.4f);
    auto mid_gray = shade(gray, 0.3f);
    auto light_gray = shade(mid_gray, 0.3f);

    auto increase_contrast = [&](Color color, Color background) {
        auto constexpr min_contrast = 2;
        if (color.contrast_ratio(background) < min_contrast) {
            color = color.inverted();
            if (color.contrast_ratio(background) > min_contrast)
                return color;
        }
        return color;
    };

    // Little heuristic that smaller things look better with more smoothness.
    float smoothness = 1.0f / (max(checkbox_rect.width(), checkbox_rect.height()) / 2);
    if (checkbox.checked() && !checkbox.indeterminate()) {
        auto background_color = enabled ? accent : mid_gray;
        painter.fill_rect_with_rounded_corners(checkbox_rect, modify_color(background_color), checkbox_radius);
        auto tick_color = increase_contrast(base, background_color);
        if (!enabled)
            tick_color = shade(tick_color, 0.5f);
        context.painter().draw_signed_distance_field(checkbox_rect, tick_color, check_mark_sdf(), smoothness);
    } else {
        auto background_color = enabled ? base : light_gray;
        auto border_thickness = max(1, checkbox_rect.width() / 10);
        painter.fill_rect_with_rounded_corners(checkbox_rect, modify_color(enabled ? gray : mid_gray), checkbox_radius);
        painter.fill_rect_with_rounded_corners(checkbox_rect.shrunken(border_thickness, border_thickness, border_thickness, border_thickness),
            enabled ? base : light_gray, max(0, checkbox_radius - border_thickness));
        if (checkbox.indeterminate()) {
            auto dash_color = increase_contrast(dark_gray, background_color);
            context.painter().draw_signed_distance_field(checkbox_rect,
                modify_color(enabled ? dash_color : shade(dash_color, 0.3f)), check_indeterminate_sdf(), smoothness);
        }
    }
}

}
