/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Timer.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ColorPicker.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, ColorInput)

namespace GUI {

ColorInput::ColorInput()
    : TextEditor(TextEditor::SingleLine)
{
    set_min_size({ 40, 22 });
    set_preferred_size({ SpecialDimension::OpportunisticGrow, 22 });
    TextEditor::on_change = [this] {
        auto parsed_color = Color::from_string(text());
        if (parsed_color.has_value())
            set_color_internal(parsed_color.value(), AllowCallback::Yes, false);
    };

    REGISTER_DEPRECATED_STRING_PROPERTY("color_picker_title", color_picker_title, set_color_picker_title);
    REGISTER_BOOL_PROPERTY("has_alpha_channel", has_alpha_channel, set_color_has_alpha_channel);
}

Gfx::IntRect ColorInput::color_rect() const
{
    auto color_box_padding = 3;
    auto color_box_size = height() - color_box_padding - color_box_padding;
    return { width() - color_box_size - color_box_padding, color_box_padding, color_box_size, color_box_size };
}

void ColorInput::set_color_internal(Color color, AllowCallback allow_callback, bool change_text)
{
    if (m_color == color)
        return;
    m_color = color;
    if (change_text)
        set_text(m_color_has_alpha_channel ? color.to_byte_string() : color.to_byte_string_without_alpha(), AllowCallback::No);
    update();
    if (allow_callback == AllowCallback::Yes && on_change)
        on_change();
}

void ColorInput::set_color(Color color, AllowCallback allow_callback)
{
    set_color_internal(color, allow_callback, true);
}

void ColorInput::mousedown_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Primary && color_rect().contains(event.position())) {
        m_may_be_color_rect_click = true;
        return;
    }

    TextEditor::mousedown_event(event);
}

void ColorInput::mouseup_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Primary) {
        bool is_color_rect_click = m_may_be_color_rect_click && color_rect().contains(event.position());
        m_may_be_color_rect_click = false;
        if (is_color_rect_click) {
            auto dialog = GUI::ColorPicker::construct(m_color, window(), m_color_picker_title);
            dialog->on_color_changed = [this](Gfx::Color color) {
                set_color(color);
            };
            dialog->set_color_has_alpha_channel(m_color_has_alpha_channel);
            dialog->exec();
            event.accept();
            return;
        }
    }
    TextEditor::mouseup_event(event);
}

void ColorInput::mousemove_event(MouseEvent& event)
{
    if (color_rect().contains(event.position())) {
        set_override_cursor(Gfx::StandardCursor::Hand);
        event.accept();
        return;
    } else {
        set_override_cursor(Gfx::StandardCursor::IBeam);
    }

    TextEditor::mousemove_event(event);
}

void ColorInput::paint_event(PaintEvent& event)
{
    TextEditor::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(color_rect(), m_color);
    painter.draw_rect(color_rect(), Color::Black);
}
}
