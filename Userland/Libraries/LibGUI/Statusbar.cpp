/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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
    set_layout<HorizontalBoxLayout>(0, 2);

    m_corner = add<ResizeCorner>();
    set_segment_count(count);

    REGISTER_STRING_PROPERTY("text", text, set_text);
    REGISTER_INT_PROPERTY("segment_count", segment_count, set_segment_count);
}

NonnullRefPtr<Statusbar::Segment> Statusbar::create_segment()
{
    auto widget = Segment::construct();
    insert_child_before(*widget, *m_corner);
    return widget;
}

void Statusbar::child_event(Core::ChildEvent& event)
{
    auto& event_to_forward = event;
    // To ensure that the ResizeCorner is always the last widget, and thus stays in the corner,
    // we replace ChildAdded events that do not request specific placement with events that request placement before the corner
    if (event.type() == Event::ChildAdded && is<Widget>(*event.child()) && !event.insertion_before_child()) {
        Core::ChildEvent new_event(Event::ChildAdded, *event.child(), m_corner.ptr());
        event_to_forward = new_event;
    }

    return Widget::child_event(event_to_forward);
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
    if (segment->mode() == Segment::Mode::Auto) {
        if (segment->restored_text().is_empty())
            segment->set_visible(false);
        else {
            constexpr auto horizontal_padding { 10 };
            auto width = font().width(segment->restored_text()) + horizontal_padding;
            segment->set_restored_width(width);
            segment->set_fixed_width(width);
        }
    } else if (segment->mode() == Segment::Mode::Fixed) {
        if (segment->max_width().is_int()) {
            segment->set_restored_width(segment->max_width().as_int());
            segment->set_fixed_width(segment->max_width());
        }
    }

    if (segment->override_text().has_value()) {
        for (size_t i = 1; i < m_segments.size(); i++) {
            if (!m_segments[i]->is_clickable())
                m_segments[i]->set_visible(false);
        }
        segment->set_text(*segment->override_text());
        segment->set_frame_style(Gfx::FrameStyle::NoFrame);
        if (segment->mode() != Segment::Mode::Proportional)
            segment->set_fixed_width(SpecialDimension::Grow);
    } else {
        for (size_t i = 1; i < m_segments.size(); i++) {
            if (!text(i).is_empty())
                m_segments[i]->set_visible(true);
        }
        segment->set_text(segment->restored_text());
        segment->set_frame_style(Gfx::FrameStyle::SunkenPanel);
        if (segment->mode() != Segment::Mode::Proportional)
            segment->set_fixed_width(segment->restored_width());
    }
}

String Statusbar::text(size_t index) const
{
    return m_segments[index]->text();
}

void Statusbar::set_text(String text)
{
    set_text(0, move(text));
}

void Statusbar::set_text(size_t index, String text)
{
    m_segments[index]->m_restored_text = move(text);
    update_segment(index);
}

void Statusbar::set_override_text(Optional<String> override_text)
{
    m_segments[0]->m_override_text = move(override_text);
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

    bool skip_vertical_lines = window()->is_maximized() && spans_entire_window_horizontally();
    Gfx::StylePainter::current().paint_frame(painter, rect(), palette(), m_style, skip_vertical_lines);

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
