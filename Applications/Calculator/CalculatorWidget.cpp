/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "CalculatorWidget.h"
#include <AK/StringBuilder.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font.h>

CalculatorWidget::CalculatorWidget()
{
    set_fill_with_background_color(true);

    m_entry = add<GUI::TextBox>();
    m_entry->set_relative_rect(5, 5, 244, 26);
    m_entry->set_text_alignment(Gfx::TextAlignment::CenterRight);
    m_entry->set_font(Gfx::Font::default_fixed_width_font());

    m_label = add<GUI::Label>();
    m_label->set_relative_rect(12, 42, 27, 27);
    m_label->set_foreground_color(Color::NamedColor::Red);
    m_label->set_frame_shadow(Gfx::FrameShadow::Sunken);
    m_label->set_frame_shape(Gfx::FrameShape::Container);
    m_label->set_frame_thickness(2);

    for (int i = 0; i < 10; i++) {
        m_digit_button[i] = add<GUI::Button>();
        auto& button = *m_digit_button[i];
        int p = i ? i + 2 : 0;
        int x = 55 + (p % 3) * 39;
        int y = 177 - (p / 3) * 33;
        button.move_to(x, y);
        button.set_foreground_color(Color::NamedColor::Blue);
        add_button(button, i);
    }

    m_mem_add_button = add<GUI::Button>();
    m_mem_add_button->move_to(9, 177);
    m_mem_add_button->set_foreground_color(Color::NamedColor::Red);
    m_mem_add_button->set_text("M+");
    m_mem_add_button->on_click = [this](auto) {
        Calculator calculator = Calculator(m_entry->text());
        double addition = calculator.evaluate();
        if (calculator.has_error()) {
            // Don't reset m_memory in this case, leave it as is.
            m_entry->set_text("Err");
        } else {
            m_memory += addition;
            m_memory_scale = 4;
        }
    };
    add_button(*m_mem_add_button);

    m_mem_save_button = add<GUI::Button>();
    m_mem_save_button->move_to(9, 144);
    m_mem_save_button->set_foreground_color(Color::NamedColor::Red);
    m_mem_save_button->set_text("MS");
    m_mem_save_button->on_click = [this](auto) {
        // Evaluate the current expression and store the result in m_memory,
        // don't set the m_entry text once the result of the calculation is found.
        Calculator calculator = Calculator(m_entry->text());
        double result = calculator.evaluate();
        if (calculator.has_error()) {
            m_entry->set_text("Err");
            m_memory = 0.0;
            m_memory_scale = 1;
        } else {
            m_memory = result;
            // This is arbitrary set at the moment, the last method of MS I had could properly
            // take a sale into account but this one can't.
            m_memory_scale = 4;
        }
    };
    add_button(*m_mem_save_button);

    m_mem_recall_button = add<GUI::Button>();
    m_mem_recall_button->move_to(9, 111);
    m_mem_recall_button->set_foreground_color(Color::NamedColor::Red);
    m_mem_recall_button->set_text("MR");
    m_mem_recall_button->on_click = [this](auto) {
        append_text_to_entry(String::number(m_memory, m_memory_scale));
    };
    add_button(*m_mem_recall_button);

    m_mem_clear_button = add<GUI::Button>();
    m_mem_clear_button->move_to(9, 78);
    m_mem_clear_button->set_foreground_color(Color::NamedColor::Red);
    m_mem_clear_button->set_text("MC");
    m_mem_clear_button->on_click = [this](auto) {
        m_memory = 0.0;
        m_memory_scale = 1;
    };
    add_button(*m_mem_clear_button);

    m_clear_button = add<GUI::Button>();
    m_clear_button->set_foreground_color(Color::NamedColor::Red);
    m_clear_button->set_text("C");
    m_clear_button->on_click = [this](auto) {
        m_entry->set_text("");
    };
    add_button(*m_clear_button);
    m_clear_button->set_relative_rect(187, 40, 60, 28);

    m_clear_error_button = add<GUI::Button>();
    m_clear_error_button->set_foreground_color(Color::NamedColor::Red);
    m_clear_error_button->set_text("CE");
    m_clear_error_button->on_click = [this](auto) {
        m_entry->set_text("");
    };
    add_button(*m_clear_error_button);
    m_clear_error_button->set_relative_rect(124, 40, 59, 28);

    m_backspace_button = add<GUI::Button>();
    m_backspace_button->set_foreground_color(Color::NamedColor::Red);
    m_backspace_button->set_text("Backspace");
    m_backspace_button->on_click = [this](auto) {
        on_backspace_pressed();
    };
    add_button(*m_backspace_button);
    m_backspace_button->set_relative_rect(55, 40, 65, 28);

    m_decimal_point_button = add<GUI::Button>();
    m_decimal_point_button->move_to(133, 177);
    m_decimal_point_button->set_foreground_color(Color::NamedColor::Blue);
    m_decimal_point_button->set_text(".");
    m_decimal_point_button->on_click = [this](auto) {
        add_button(*m_decimal_point_button);
    };

    m_sign_button = add<GUI::Button>();
    m_sign_button->move_to(94, 177);
    m_sign_button->set_foreground_color(Color::NamedColor::Blue);
    m_sign_button->set_text("+/-");
    add_button(*m_sign_button);

    m_add_button = add<GUI::Button>();
    m_add_button->move_to(172, 177);
    m_add_button->set_foreground_color(Color::NamedColor::Red);
    m_add_button->set_text("+");
    add_button(*m_add_button, "+");

    m_subtract_button = add<GUI::Button>();
    m_subtract_button->move_to(172, 144);
    m_subtract_button->set_foreground_color(Color::NamedColor::Red);
    m_subtract_button->set_text("-");
    add_button(*m_subtract_button, "-");

    m_multiply_button = add<GUI::Button>();
    m_multiply_button->move_to(172, 111);
    m_multiply_button->set_foreground_color(Color::NamedColor::Red);
    m_multiply_button->set_text("*");
    add_button(*m_multiply_button, "*");

    m_divide_button = add<GUI::Button>();
    m_divide_button->move_to(172, 78);
    m_divide_button->set_foreground_color(Color::NamedColor::Red);
    m_divide_button->set_text("/");
    add_button(*m_divide_button, "/");

    m_sqrt_button = add<GUI::Button>();
    m_sqrt_button->move_to(211, 78);
    m_sqrt_button->set_foreground_color(Color::NamedColor::Blue);
    m_sqrt_button->set_text("sqrt");
    add_button(*m_sqrt_button);

    m_inverse_button = add<GUI::Button>();
    m_inverse_button->move_to(211, 144);
    m_inverse_button->set_foreground_color(Color::NamedColor::Blue);
    m_inverse_button->set_text("1/x");
    add_button(*m_inverse_button);

    m_percent_button = add<GUI::Button>();
    m_percent_button->move_to(211, 111);
    m_percent_button->set_foreground_color(Color::NamedColor::Blue);
    m_percent_button->set_text("%");
    add_button(*m_percent_button);

    m_equals_button = add<GUI::Button>();
    m_equals_button->move_to(211, 177);
    m_equals_button->set_foreground_color(Color::NamedColor::Red);
    m_equals_button->set_text("=");
    m_equals_button->on_click = [this](auto) {
        on_equals_pressed();
    };
    add_button(*m_equals_button);
}

void CalculatorWidget::on_equals_pressed()
{
    if (m_entry->text().is_empty()) {
        m_entry->set_text("0");
        return;
    }
    if (m_entry->text() == "()") {
        m_entry->set_text("0");
        return;
    }
    Calculator calculator = Calculator(m_entry->text());
    double result = calculator.evaluate();
    if (calculator.has_error()) {
        m_entry->set_text("Err");
    } else {
        m_entry->set_text(String::number(result, 5));
    };
}

CalculatorWidget::~CalculatorWidget() = default;

void CalculatorWidget::add_button(GUI::Button& button, int digit)
{
    add_button(button);
    button.set_text(String::number(digit));
    button.on_click = [this, digit](auto) {
        append_text_to_entry(String::number(digit));
    };
}

void CalculatorWidget::add_button(GUI::Button& button, String symbol)
{
    add_button(button);
    button.on_click = [this, symbol](auto) {
        append_text_to_entry(symbol);
    };
}

void CalculatorWidget::add_button(GUI::Button& button)
{
    button.resize(35, 28);
}

void CalculatorWidget::keydown_event(GUI::KeyEvent& event)
{
    //Clear button selection when we are typing
    m_equals_button->set_focus(true);
    m_equals_button->set_focus(false);

    switch (event.key()) {
    case KeyCode::Key_Return:
    case KeyCode::Key_Equal:
        on_equals_pressed();
        break;
    case KeyCode::Key_Backspace:
        on_backspace_pressed();
        break;
    case KeyCode::Key_0:
    case KeyCode::Key_1:
    case KeyCode::Key_2:
    case KeyCode::Key_3:
    case KeyCode::Key_4:
    case KeyCode::Key_5:
    case KeyCode::Key_6:
    case KeyCode::Key_7:
    case KeyCode::Key_8:
    case KeyCode::Key_9:
    case KeyCode::Key_Period:
    case KeyCode::Key_Plus:
    case KeyCode::Key_Minus:
    case KeyCode::Key_Asterisk:
    case KeyCode::Key_Slash:
    case KeyCode::Key_LeftParen:
    case KeyCode::Key_RightParen:
        append_text_to_entry(event.text());
    }
}

void CalculatorWidget::append_text_to_entry(String text)
{
    String current_text = m_entry->text();

    // Clear the error text
    if (current_text == "E")
        current_text = "";

    StringBuilder string_builder = StringBuilder(current_text.length() + text.length());
    string_builder.append(current_text);
    string_builder.append(text);
    m_entry->set_text(string_builder.to_string());
}

void CalculatorWidget::on_backspace_pressed()
{
    if (m_entry->text().is_empty())
        return;
    String current_text = m_entry->text();
    String new_text = current_text.substring(0, current_text.length() - 1);
    m_entry->set_text(new_text);
}
