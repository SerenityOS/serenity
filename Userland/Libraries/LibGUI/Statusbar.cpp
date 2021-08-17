/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ResizeCorner.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, Statusbar)

namespace GUI {

Statusbar::Statusbar(int label_count)
{
    set_fixed_height(18);
    set_layout<HorizontalBoxLayout>();
    layout()->set_margins(0);
    layout()->set_spacing(2);

    m_corner = add<ResizeCorner>();
    set_label_count(label_count);

    REGISTER_STRING_PROPERTY("text", text, set_text);
    REGISTER_INT_PROPERTY("label_count", label_count, set_label_count);
}

Statusbar::~Statusbar()
{
}

NonnullRefPtr<Label> Statusbar::create_label()
{
    auto label = Label::construct();
    insert_child_before(*label, *m_corner);
    label->set_frame_shadow(Gfx::FrameShadow::Sunken);
    label->set_frame_shape(Gfx::FrameShape::Panel);
    label->set_frame_thickness(1);
    label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    label->set_text_wrapping(Gfx::TextWrapping::DontWrap);
    return label;
}

void Statusbar::set_text(String text)
{
    set_text(0, move(text));
}

String Statusbar::text() const
{
    return text(0);
}

void Statusbar::set_text(size_t index, String text)
{
    m_segments.at(index).text = move(text);
    update_label(index);
}

void Statusbar::set_label_count(size_t label_count)
{
    if (label_count <= 1)
        label_count = 1;

    for (auto i = m_segments.size(); i < label_count; i++) {
        m_segments.append(Segment {
            .label = create_label(),
            .text = {},
            .override_text = {},
        });
    }
}

void Statusbar::update_label(size_t index)
{
    auto& segment = m_segments.at(index);

    if (segment.override_text.is_null()) {
        segment.label->set_frame_shadow(Gfx::FrameShadow::Sunken);
        segment.label->set_frame_shape(Gfx::FrameShape::Panel);
        segment.label->set_text(segment.text);
    } else {
        segment.label->set_frame_shadow(Gfx::FrameShadow::Plain);
        segment.label->set_frame_shape(Gfx::FrameShape::NoFrame);
        segment.label->set_text(segment.override_text);
    }
}

String Statusbar::text(size_t index) const
{
    return m_segments.at(index).label->text();
}

void Statusbar::set_override_text(String override_text)
{
    set_override_text(0, move(override_text));
}

void Statusbar::set_override_text(size_t index, String override_text)
{
    m_segments.at(index).override_text = move(override_text);
    update_label(index);
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
}
