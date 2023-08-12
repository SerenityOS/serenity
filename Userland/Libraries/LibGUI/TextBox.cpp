/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Palette.h>
#include <LibGfx/TextAttributes.h>

REGISTER_WIDGET(GUI, TextBox)
REGISTER_WIDGET(GUI, PasswordBox)
REGISTER_WIDGET(GUI, UrlBox)

namespace GUI {

TextBox::TextBox()
    : TextEditor(TextEditor::SingleLine)
{
    set_min_size({ SpecialDimension::Shrink });
    set_preferred_size({ SpecialDimension::OpportunisticGrow, SpecialDimension::Shrink });
}

void TextBox::keydown_event(GUI::KeyEvent& event)
{
    TextEditor::keydown_event(event);

    if (event.key() == Key_Up) {
        if (on_up_pressed)
            on_up_pressed();

        if (has_no_history() || !can_go_backwards_in_history())
            return;

        if (m_history_index >= static_cast<int>(m_history.size()))
            m_saved_input = text();

        m_history_index--;
        set_text(m_history[m_history_index]);
    } else if (event.key() == Key_Down) {
        if (on_down_pressed)
            on_down_pressed();

        if (has_no_history())
            return;

        if (can_go_forwards_in_history()) {
            m_history_index++;
            set_text(m_history[m_history_index]);
        } else if (m_history_index < static_cast<int>(m_history.size())) {
            m_history_index++;
            set_text(m_saved_input);
        }
    }
}

void TextBox::add_current_text_to_history()
{
    if (!m_history_enabled)
        return;

    auto input = text();
    if (m_history.is_empty() || m_history.last() != input)
        add_input_to_history(input);
    m_history_index = static_cast<int>(m_history.size());
    m_saved_input = {};
}

void TextBox::add_input_to_history(DeprecatedString input)
{
    m_history.append(move(input));
    m_history_index++;
}

constexpr u32 password_box_substitution_code_point = '*';

PasswordBox::PasswordBox()
    : TextBox()
{
    set_substitution_code_point(password_box_substitution_code_point);
    set_text_is_secret(true);
    REGISTER_BOOL_PROPERTY("show_reveal_button", is_showing_reveal_button, set_show_reveal_button);
}

Gfx::IntRect PasswordBox::reveal_password_button_rect() const
{
    constexpr i32 button_box_margin = 3;
    auto button_box_size = height() - button_box_margin - button_box_margin;
    return { width() - button_box_size - button_box_margin, button_box_margin, button_box_size, button_box_size };
}

void PasswordBox::paint_event(PaintEvent& event)
{
    TextBox::paint_event(event);

    if (is_showing_reveal_button()) {
        auto button_rect = reveal_password_button_rect();

        Painter painter(*this);
        painter.add_clip_rect(event.rect());

        auto icon_color = palette().button_text();
        if (substitution_code_point().has_value())
            icon_color = palette().disabled_text_front();

        i32 dot_indicator_padding = height() / 5;

        Gfx::IntRect dot_indicator_rect = { button_rect.x() + dot_indicator_padding, button_rect.y() + dot_indicator_padding, button_rect.width() - dot_indicator_padding * 2, button_rect.height() - dot_indicator_padding * 2 };
        painter.fill_ellipse(dot_indicator_rect, icon_color);

        Gfx::IntPoint arc_start_point { dot_indicator_rect.x() - dot_indicator_padding / 2, dot_indicator_rect.y() + dot_indicator_padding / 2 };
        Gfx::IntPoint arc_end_point = { dot_indicator_rect.right() - 1 + dot_indicator_padding / 2, dot_indicator_rect.top() + dot_indicator_padding / 2 };
        Gfx::IntPoint arc_center_point = { dot_indicator_rect.center().x(), dot_indicator_rect.top() - dot_indicator_padding };
        painter.draw_quadratic_bezier_curve(arc_center_point, arc_start_point, arc_end_point, icon_color, 1);
    }
}

void PasswordBox::mousedown_event(GUI::MouseEvent& event)
{
    if (is_showing_reveal_button() && reveal_password_button_rect().contains(event.position())) {
        Optional<u32> next_substitution_code_point;
        if (!substitution_code_point().has_value())
            next_substitution_code_point = password_box_substitution_code_point;
        set_substitution_code_point(next_substitution_code_point);
    } else {
        TextBox::mousedown_event(event);
    }
}

UrlBox::UrlBox()
    : TextBox()
{
    set_auto_focusable(false);
    on_change = [this] {
        highlight_url();
    };
}

void UrlBox::focusout_event(GUI::FocusEvent& event)
{
    set_focus_transition(true);

    highlight_url();
    TextBox::focusout_event(event);
}

void UrlBox::focusin_event(GUI::FocusEvent& event)
{
    highlight_url();
    TextBox::focusin_event(event);
}

void UrlBox::mousedown_event(GUI::MouseEvent& event)
{
    if (is_displayonly())
        return;

    if (event.button() != MouseButton::Primary)
        return;

    if (is_focus_transition()) {
        TextBox::select_current_line();

        set_focus_transition(false);
    } else {
        TextBox::mousedown_event(event);
    }
}

void UrlBox::highlight_url()
{
    auto url = AK::URL::create_with_url_or_path(text());
    Vector<GUI::TextDocumentSpan> spans;

    if (url.is_valid() && !is_focused()) {
        if (url.scheme() == "http" || url.scheme() == "https" || url.scheme() == "gemini") {
            auto serialized_host = url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string();
            auto host_start = url.scheme().bytes_as_string_view().length() + 3;
            auto host_length = serialized_host.length();

            // FIXME: Maybe add a generator to use https://publicsuffix.org/list/public_suffix_list.dat
            //        for now just highlight the whole host

            Gfx::TextAttributes default_format;
            default_format.color = palette().color(Gfx::ColorRole::PlaceholderText);
            spans.append({
                { { 0, 0 }, { 0, host_start } },
                default_format,
            });

            Gfx::TextAttributes host_format;
            host_format.color = palette().color(Gfx::ColorRole::BaseText);
            spans.append({
                { { 0, host_start }, { 0, host_start + host_length } },
                host_format,
            });

            spans.append({
                { { 0, host_start + host_length }, { 0, text().length() } },
                default_format,
            });
        } else if (url.scheme() == "file") {
            Gfx::TextAttributes scheme_format;
            scheme_format.color = palette().color(Gfx::ColorRole::PlaceholderText);
            spans.append({
                { { 0, 0 }, { 0, url.scheme().bytes_as_string_view().length() + 3 } },
                scheme_format,
            });
        }
    }

    document().set_spans(0, move(spans));
    update();
}

}
