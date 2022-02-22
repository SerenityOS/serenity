/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ResizeCorner.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, Statusbar)

namespace GUI {

Statusbar::Statusbar(int count)
{
    set_fixed_height(18);
    set_layout<HorizontalBoxLayout>();
    layout()->set_margins(0);
    layout()->set_spacing(2);

    m_corner = add<ResizeCorner>();
    set_segment_count(count);

    REGISTER_STRING_PROPERTY("text", text, set_text);
    REGISTER_INT_PROPERTY("segment_count", segment_count, set_segment_count);
}

Statusbar::~Statusbar()
{
}

NonnullRefPtr<Statusbar::Segment> Statusbar::create_segment()
{
    auto widget = Segment::construct();
    insert_child_before(*widget, *m_corner);
    return widget;
}

void Statusbar::set_segment_count(size_t count)
{
    if (count <= 1)
        count = 1;

    for (auto i = m_segments.size(); i < count; i++) {
        auto segment = create_segment();
        m_segments.append(move(segment));
    }
}

void Statusbar::update_segment(size_t index)
{
    auto& segment = m_segments.at(index);
    if (segment.mode() == Segment::Mode::Auto) {
        if (segment.restored_text().is_empty())
            segment.set_visible(false);
        else {
            constexpr auto horizontal_padding { 10 };
            auto width = font().width(segment.restored_text()) + horizontal_padding;
            segment.set_restored_width(width);
            segment.set_fixed_width(width);
        }
    } else if (segment.mode() == Segment::Mode::Fixed) {
        if (segment.max_width() != -1)
            segment.set_restored_width(segment.max_width());
        segment.set_fixed_width(segment.max_width());
    }

    if (segment.override_text().is_null()) {
        for (size_t i = 1; i < m_segments.size(); i++) {
            if (!text(i).is_empty())
                m_segments[i].set_visible(true);
        }
        segment.set_text(segment.restored_text());
        segment.set_frame_shape(Gfx::FrameShape::Panel);
        if (segment.mode() != Segment::Mode::Proportional)
            segment.set_fixed_width(segment.restored_width());
    } else {
        for (size_t i = 1; i < m_segments.size(); i++) {
            if (!m_segments[i].is_clickable())
                m_segments[i].set_visible(false);
        }
        segment.set_text(segment.override_text());
        segment.set_frame_shape(Gfx::FrameShape::NoFrame);
        if (segment.mode() != Segment::Mode::Proportional)
            segment.set_fixed_width(-1);
    }
}

String Statusbar::text(size_t index) const
{
    return m_segments.at(index).text();
}

void Statusbar::set_text(String text)
{
    set_text(0, move(text));
}

void Statusbar::set_text(size_t index, String text)
{
    m_segments.at(index).m_restored_text = move(text);
    update_segment(index);
}

void Statusbar::set_override_text(String override_text)
{
    m_segments.at(0).m_override_text = move(override_text);
    update_segment(0);
}

void Statusbar::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(rect(), palette().button());
}

void Statusbar::resize_event(ResizeEvent& event)
{
    if (auto* window = this->window()) {
        m_corner->set_visible(window->is_resizable() && !window->is_maximized());
    }

    Widget::resize_event(event);
}

Statusbar::Segment::Segment()
{
    set_fixed_height(18);
    set_focus_policy(GUI::FocusPolicy::NoFocus);
    set_button_style(Gfx::ButtonStyle::Tray);
    set_text_alignment(Gfx::TextAlignment::CenterLeft);
}

void Statusbar::Segment::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::StylePainter::current().paint_frame(painter, rect(), palette(), m_shape, Gfx::FrameShadow::Sunken, m_thickness, spans_entire_window_horizontally());

    if (is_clickable())
        Button::paint_event(event);
    else if (!text().is_empty())
        painter.draw_text(rect().shrunken(font().max_glyph_width(), 0), text(), text_alignment(), palette().color(foreground_role()), Gfx::TextElision::Right, Gfx::TextWrapping::DontWrap);
}

void Statusbar::Segment::mousedown_event(MouseEvent& event)
{
    if (!is_clickable())
        return;
    Button::mousedown_event(event);
}

void Statusbar::Segment::mouseup_event(MouseEvent& event)
{
    if (!is_clickable())
        return;
    Button::mouseup_event(event);
}

}
