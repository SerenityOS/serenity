/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/TextLayout.h>
#include <LibGfx/TextWrapping.h>

REGISTER_WIDGET(GUI, Label)

namespace GUI {

Label::Label(DeprecatedString text)
    : m_text(move(text))
{
    REGISTER_TEXT_ALIGNMENT_PROPERTY("text_alignment", text_alignment, set_text_alignment);
    REGISTER_TEXT_WRAPPING_PROPERTY("text_wrapping", text_wrapping, set_text_wrapping);

    set_preferred_size({ SpecialDimension::OpportunisticGrow });
    set_min_height(22);

    set_frame_thickness(0);
    set_frame_shadow(Gfx::FrameShadow::Plain);
    set_frame_shape(Gfx::FrameShape::NoFrame);

    set_foreground_role(Gfx::ColorRole::WindowText);

    REGISTER_STRING_PROPERTY("text", text, set_text);
    REGISTER_BOOL_PROPERTY("autosize", is_autosize, set_autosize);
    REGISTER_WRITE_ONLY_STRING_PROPERTY("icon", set_icon_from_path);
}

void Label::set_autosize(bool autosize, size_t padding)
{
    if (m_autosize == autosize && m_autosize_padding == padding)
        return;
    m_autosize = autosize;
    m_autosize_padding = padding;
    if (m_autosize)
        size_to_fit();
}

void Label::set_icon(Gfx::Bitmap const* icon)
{
    if (m_icon == icon)
        return;
    m_icon = icon;
    update();
}

void Label::set_icon_from_path(DeprecatedString const& path)
{
    auto maybe_bitmap = Gfx::Bitmap::try_load_from_file(path);
    if (maybe_bitmap.is_error()) {
        dbgln("Unable to load bitmap `{}` for label icon", path);
        return;
    }
    set_icon(maybe_bitmap.release_value());
}

void Label::set_text(DeprecatedString text)
{
    if (text == m_text)
        return;
    m_text = move(text);

    if (m_autosize)
        size_to_fit();
    update();
    did_change_text();
}

Gfx::IntRect Label::text_rect() const
{
    return frame_inner_rect().shrunken(frame_thickness() > 0 ? font().glyph_width('x') : 0, 0);
}

void Label::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (m_icon) {
        if (m_should_stretch_icon) {
            painter.draw_scaled_bitmap(frame_inner_rect(), *m_icon, m_icon->rect());
        } else {
            auto icon_location = frame_inner_rect().center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2));
            painter.blit(icon_location, *m_icon, m_icon->rect());
        }
    }

    if (text().is_empty())
        return;

    auto text_rect = this->text_rect();
    if (is_enabled()) {
        painter.draw_text(text_rect, text(), text_alignment(), palette().color(foreground_role()), Gfx::TextElision::Right, text_wrapping());
    } else {
        painter.draw_text(text_rect.translated(1, 1), text(), font(), text_alignment(), palette().disabled_text_back(), Gfx::TextElision::Right, text_wrapping());
        painter.draw_text(text_rect, text(), font(), text_alignment(), palette().disabled_text_front(), Gfx::TextElision::Right, text_wrapping());
    }
}

void Label::size_to_fit()
{
    set_fixed_width(font().width(m_text) + m_autosize_padding * 2);
}

int Label::text_calculated_preferred_height() const
{
    return int(AK::ceil(Gfx::TextLayout(&font(), Utf8View { m_text }, text_rect().to_type<float>()).bounding_rect(Gfx::TextWrapping::Wrap, Gfx::Painter::LINE_SPACING).height()));
}

Optional<UISize> Label::calculated_preferred_size() const
{
    return GUI::UISize(SpecialDimension::Grow, text_calculated_preferred_height());
}
}
