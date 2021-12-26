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
#include <AK/Assertions.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

CalculatorWidget::CalculatorWidget()
{
    set_fill_with_background_color(true);

    m_entry = add<GUI::TextBox>();
    m_entry->set_relative_rect(5, 5, 244, 26);
    m_entry->set_text_alignment(Gfx::TextAlignment::CenterRight);
    m_entry->set_font(Gfx::Font::default_fixed_width_font());

    m_label = add<GUI::Label>();
    m_label->set_relative_rect(12, 42, 27, 27);
    m_label->set_frame_shadow(Gfx::FrameShadow::Sunken);
    m_label->set_frame_shape(Gfx::FrameShape::Container);
    m_label->set_frame_thickness(2);
    auto label_palette = m_label->palette();
    label_palette.set_color(Gfx::ColorRole::WindowText, Color::Red);
    m_label->set_palette(label_palette);

    update_display();

    for (int i = 0; i < 10; i++) {
        m_digit_button[i] = add<GUI::Button>();
        auto& button = *m_digit_button[i];
        int p = i ? i + 2 : 0;
        int x = 55 + (p % 3) * 39;
        int y = 177 - (p / 3) * 33;
        button.move_to(x, y);
        add_digit_button(button, i);
    }

    m_mem_add_button = add<GUI::Button>();
    m_mem_add_button->move_to(9, 177);
    m_mem_add_button->set_text("M+");
    add_operation_button(*m_mem_add_button, Calculator::Operation::MemAdd);

    m_mem_save_button = add<GUI::Button>();
    m_mem_save_button->move_to(9, 144);
    m_mem_save_button->set_text("MS");
    add_operation_button(*m_mem_save_button, Calculator::Operation::MemSave);

    m_mem_recall_button = add<GUI::Button>();
    m_mem_recall_button->move_to(9, 111);
    m_mem_recall_button->set_text("MR");
    add_operation_button(*m_mem_recall_button, Calculator::Operation::MemRecall);

    m_mem_clear_button = add<GUI::Button>();
    m_mem_clear_button->move_to(9, 78);
    m_mem_clear_button->set_text("MC");
    add_operation_button(*m_mem_clear_button, Calculator::Operation::MemClear);

    m_clear_button = add<GUI::Button>();
    m_clear_button->set_text("C");
    m_clear_button->on_click = [this](auto) {
        m_keypad.set_value(0.0);
        m_calculator.clear_operation();
        update_display();
    };
    add_button(*m_clear_button, Color::Red);
    m_clear_button->set_relative_rect(187, 40, 60, 28);

    m_clear_error_button = add<GUI::Button>();
    m_clear_error_button->set_text("CE");
    m_clear_error_button->on_click = [this](auto) {
        m_calculator.clear_error();
        update_display();
    };
    add_button(*m_clear_error_button, Color::Red);
    m_clear_error_button->set_relative_rect(124, 40, 59, 28);

    m_backspace_button = add<GUI::Button>();
    m_backspace_button->set_text("Backspace");
    m_backspace_button->on_click = [this](auto) {
        m_keypad.type_backspace();
        update_display();
    };
    add_button(*m_backspace_button, Color::Red);
    m_backspace_button->set_relative_rect(55, 40, 65, 28);

    m_decimal_point_button = add<GUI::Button>();
    m_decimal_point_button->move_to(133, 177);
    m_decimal_point_button->set_text(".");
    m_decimal_point_button->on_click = [this](auto) {
        m_keypad.type_decimal_point();
        update_display();
    };
    add_button(*m_decimal_point_button, Color::Blue);

    m_sign_button = add<GUI::Button>();
    m_sign_button->move_to(94, 177);
    m_sign_button->set_text("+/-");
    add_operation_button(*m_sign_button, Calculator::Operation::ToggleSign, Color::Blue);

    m_add_button = add<GUI::Button>();
    m_add_button->move_to(172, 177);
    m_add_button->set_text("+");
    add_operation_button(*m_add_button, Calculator::Operation::Add);

    m_subtract_button = add<GUI::Button>();
    m_subtract_button->move_to(172, 144);
    m_subtract_button->set_text("-");
    add_operation_button(*m_subtract_button, Calculator::Operation::Subtract);

    m_multiply_button = add<GUI::Button>();
    m_multiply_button->move_to(172, 111);
    m_multiply_button->set_text("*");
    add_operation_button(*m_multiply_button, Calculator::Operation::Multiply);

    m_divide_button = add<GUI::Button>();
    m_divide_button->move_to(172, 78);
    m_divide_button->set_text("/");
    add_operation_button(*m_divide_button, Calculator::Operation::Divide);

    m_sqrt_button = add<GUI::Button>();
    m_sqrt_button->move_to(211, 78);
    m_sqrt_button->set_text("sqrt");
    add_operation_button(*m_sqrt_button, Calculator::Operation::Sqrt, Color::Blue);

    m_inverse_button = add<GUI::Button>();
    m_inverse_button->move_to(211, 144);
    m_inverse_button->set_text("1/x");
    add_operation_button(*m_inverse_button, Calculator::Operation::Inverse, Color::Blue);

    m_percent_button = add<GUI::Button>();
    m_percent_button->move_to(211, 111);
    m_percent_button->set_text("%");
    add_operation_button(*m_percent_button, Calculator::Operation::Percent, Color::Blue);

    m_equals_button = add<GUI::Button>();
    m_equals_button->move_to(211, 177);
    m_equals_button->set_text("=");
    m_equals_button->on_click = [this](auto) {
        double argument = m_keypad.value();
        double res = m_calculator.finish_operation(argument);
        m_keypad.set_value(res);
        update_display();
    };
    add_button(*m_equals_button, Color::Red);
}

CalculatorWidget::~CalculatorWidget()
{
}

void CalculatorWidget::add_operation_button(GUI::Button& button, Calculator::Operation operation, Color text_color)
{
    add_button(button, text_color);
    button.on_click = [this, operation](auto) {
        double argument = m_keypad.value();
        double res = m_calculator.begin_operation(operation, argument);
        m_keypad.set_value(res);
        update_display();
    };
}

void CalculatorWidget::add_digit_button(GUI::Button& button, int digit)
{
    add_button(button, Color::Blue);
    button.set_text(String::number(digit));
    button.on_click = [this, digit](auto) {
        m_keypad.type_digit(digit);
        update_display();
    };
}

void CalculatorWidget::add_button(GUI::Button& button, Color text_color)
{
    button.resize(35, 28);
    auto palette = button.palette();
    palette.set_color(Gfx::ColorRole::ButtonText, text_color);
    button.set_palette(palette);
}

void CalculatorWidget::update_display()
{
    m_entry->set_text(m_keypad.to_string());
    if (m_calculator.has_error())
        m_label->set_text("E");
    else
        m_label->set_text("");
}

void CalculatorWidget::keydown_event(GUI::KeyEvent& event)
{
    //Clear button selection when we are typing
    m_equals_button->set_focus(true);
    m_equals_button->set_focus(false);

    if (event.key() == KeyCode::Key_Return) {
        m_keypad.set_value(m_calculator.finish_operation(m_keypad.value()));

    } else if (event.key() >= KeyCode::Key_0 && event.key() <= KeyCode::Key_9) {
        m_keypad.type_digit(atoi(event.text().characters()));

    } else if (event.key() == KeyCode::Key_Period) {
        m_keypad.type_decimal_point();

    } else if (event.key() == KeyCode::Key_Escape) {
        m_keypad.set_value(0.0);
        m_calculator.clear_operation();

    } else if (event.key() == KeyCode::Key_Backspace) {
        m_keypad.type_backspace();

    } else {
        Calculator::Operation operation;

        switch (event.key()) {
        case KeyCode::Key_Plus:
            operation = Calculator::Operation::Add;
            break;
        case KeyCode::Key_Minus:
            operation = Calculator::Operation::Subtract;
            break;
        case KeyCode::Key_Asterisk:
            operation = Calculator::Operation::Multiply;
            break;
        case KeyCode::Key_Slash:
            operation = Calculator::Operation::Divide;
            break;
        case KeyCode::Key_Percent:
            operation = Calculator::Operation::Percent;
            break;
        default:
            return;
        }

        m_keypad.set_value(m_calculator.begin_operation(operation, m_keypad.value()));
    }

    update_display();
}
