/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Glenford Williams <gw_dev@outlook.com>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalculatorWidget.h"
#include "KeypadValue.h"
#include <Applications/Calculator/CalculatorGML.h>
#include <LibCore/Event.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

CalculatorWidget::CalculatorWidget()
{
    load_from_gml(calculator_gml);

    m_entry = *find_descendant_of_type_named<GUI::TextBox>("entry_textbox");
    m_entry->set_relative_rect(5, 5, 244, 26);
    m_entry->set_text_alignment(Gfx::TextAlignment::CenterRight);

    m_label = *find_descendant_of_type_named<GUI::Label>("label");

    m_label->set_frame_shadow(Gfx::FrameShadow::Sunken);
    m_label->set_frame_shape(Gfx::FrameShape::Container);
    m_label->set_frame_thickness(2);

    for (int i = 0; i < 10; i++) {
        m_digit_button[i] = *find_descendant_of_type_named<GUI::Button>(String::formatted("{}_button", i));
        add_digit_button(*m_digit_button[i], i);
    }

    m_mem_add_button = *find_descendant_of_type_named<GUI::Button>("mem_add_button");
    add_operation_button(*m_mem_add_button, Calculator::Operation::MemAdd);

    m_mem_save_button = *find_descendant_of_type_named<GUI::Button>("mem_save_button");
    add_operation_button(*m_mem_save_button, Calculator::Operation::MemSave);

    m_mem_recall_button = *find_descendant_of_type_named<GUI::Button>("mem_recall_button");
    add_operation_button(*m_mem_recall_button, Calculator::Operation::MemRecall);

    m_mem_clear_button = *find_descendant_of_type_named<GUI::Button>("mem_clear_button");
    add_operation_button(*m_mem_clear_button, Calculator::Operation::MemClear);

    m_clear_button = *find_descendant_of_type_named<GUI::Button>("clear_button");
    m_clear_button->on_click = [this](auto) {
        m_keypad.set_value(0.0);
        m_calculator.clear_operation();
        update_display();
    };

    m_clear_error_button = *find_descendant_of_type_named<GUI::Button>("clear_error_button");
    m_clear_error_button->on_click = [this](auto) {
        m_keypad.set_value(0.0);
        update_display();
    };

    m_backspace_button = *find_descendant_of_type_named<GUI::Button>("backspace_button");
    m_backspace_button->on_click = [this](auto) {
        m_keypad.type_backspace();
        update_display();
    };

    m_decimal_point_button = *find_descendant_of_type_named<GUI::Button>("decimal_button");
    m_decimal_point_button->on_click = [this](auto) {
        m_keypad.type_decimal_point();
        update_display();
    };

    m_sign_button = *find_descendant_of_type_named<GUI::Button>("sign_button");
    add_operation_button(*m_sign_button, Calculator::Operation::ToggleSign);

    m_add_button = *find_descendant_of_type_named<GUI::Button>("add_button");
    add_operation_button(*m_add_button, Calculator::Operation::Add);

    m_subtract_button = *find_descendant_of_type_named<GUI::Button>("subtract_button");
    add_operation_button(*m_subtract_button, Calculator::Operation::Subtract);

    m_multiply_button = *find_descendant_of_type_named<GUI::Button>("multiply_button");
    add_operation_button(*m_multiply_button, Calculator::Operation::Multiply);

    m_divide_button = *find_descendant_of_type_named<GUI::Button>("divide_button");
    add_operation_button(*m_divide_button, Calculator::Operation::Divide);

    m_sqrt_button = *find_descendant_of_type_named<GUI::Button>("sqrt_button");
    add_operation_button(*m_sqrt_button, Calculator::Operation::Sqrt);

    m_inverse_button = *find_descendant_of_type_named<GUI::Button>("inverse_button");
    add_operation_button(*m_inverse_button, Calculator::Operation::Inverse);

    m_percent_button = *find_descendant_of_type_named<GUI::Button>("mod_button");
    add_operation_button(*m_percent_button, Calculator::Operation::Percent);

    m_equals_button = *find_descendant_of_type_named<GUI::Button>("equal_button");
    m_equals_button->on_click = [this](auto) {
        KeypadValue argument = m_keypad.value();
        KeypadValue res = m_calculator.finish_operation(argument);
        m_keypad.set_value(res);
        update_display();
    };
}

void CalculatorWidget::perform_operation(Calculator::Operation operation)
{
    KeypadValue argument = m_keypad.value();
    KeypadValue res = m_calculator.begin_operation(operation, argument);
    m_keypad.set_value(res);
    update_display();
}

void CalculatorWidget::add_operation_button(GUI::Button& button, Calculator::Operation operation)
{
    button.on_click = [this, operation](auto) {
        perform_operation(operation);
    };
}

void CalculatorWidget::add_digit_button(GUI::Button& button, int digit)
{
    button.on_click = [this, digit](auto) {
        m_keypad.type_digit(digit);
        update_display();
    };
}

String CalculatorWidget::get_entry()
{
    return m_entry->text();
}

void CalculatorWidget::set_entry(KeypadValue value)
{
    m_keypad.set_value(value);
    update_display();
}

void CalculatorWidget::mimic_pressed_button(RefPtr<GUI::Button> button)
{
    constexpr int TIMER_MS = 80;

    if (!m_mimic_pressed_button.is_null())
        m_mimic_pressed_button->set_mimic_pressed(false);

    button->set_mimic_pressed(true);
    m_mimic_pressed_button = button;

    stop_timer();
    start_timer(TIMER_MS, Core::TimerShouldFireWhenNotVisible::Yes);
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
    if (event.key() == KeyCode::Key_Return || event.key() == KeyCode::Key_Equal) {
        m_keypad.set_value(m_calculator.finish_operation(m_keypad.value()));
        mimic_pressed_button(m_equals_button);
    } else if (event.code_point() >= '0' && event.code_point() <= '9') {
        u32 digit = event.code_point() - '0';
        m_keypad.type_digit(digit);
        mimic_pressed_button(m_digit_button[digit]);
    } else if (event.code_point() == '.') {
        m_keypad.type_decimal_point();
        mimic_pressed_button(m_decimal_point_button);
    } else if (event.key() == KeyCode::Key_Escape || event.key() == KeyCode::Key_Delete) {
        m_keypad.set_value(0.0);
        m_calculator.clear_operation();
        mimic_pressed_button(m_clear_button);
    } else if (event.key() == KeyCode::Key_Backspace) {
        m_keypad.type_backspace();
        mimic_pressed_button(m_backspace_button);
    } else if (event.key() == KeyCode::Key_Backslash) {
        perform_operation(Calculator::Operation::ToggleSign);
        mimic_pressed_button(m_sign_button);
    } else if (event.key() == KeyCode::Key_S) {
        perform_operation(Calculator::Operation::Sqrt);
        mimic_pressed_button(m_sqrt_button);
    } else if (event.key() == KeyCode::Key_Percent) {
        perform_operation(Calculator::Operation::Percent);
        mimic_pressed_button(m_percent_button);
    } else if (event.key() == KeyCode::Key_I) {
        perform_operation(Calculator::Operation::Inverse);
        mimic_pressed_button(m_inverse_button);
    } else {
        Calculator::Operation operation;

        switch (event.code_point()) {
        case '+':
            operation = Calculator::Operation::Add;
            mimic_pressed_button(m_add_button);
            break;
        case '-':
            operation = Calculator::Operation::Subtract;
            mimic_pressed_button(m_subtract_button);
            break;
        case '*':
            operation = Calculator::Operation::Multiply;
            mimic_pressed_button(m_multiply_button);
            break;
        case '/':
            operation = Calculator::Operation::Divide;
            mimic_pressed_button(m_divide_button);
            break;
        case '%':
            operation = Calculator::Operation::Percent;
            mimic_pressed_button(m_percent_button);
            break;
        default:
            return;
        }

        m_keypad.set_value(m_calculator.begin_operation(operation, m_keypad.value()));
    }

    update_display();
}

void CalculatorWidget::timer_event(Core::TimerEvent&)
{
    if (!m_mimic_pressed_button.is_null())
        m_mimic_pressed_button->set_mimic_pressed(false);
}
