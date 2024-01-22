/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Glenford Williams <gw_dev@outlook.com>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalculatorWidget.h"
#include <LibCrypto/BigFraction/BigFraction.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>

namespace Calculator {

ErrorOr<void> CalculatorWidget::initialize()
{
    m_entry = *find_descendant_of_type_named<GUI::TextBox>("entry_textbox");
    // FIXME: Use GML for this.
    m_entry->set_relative_rect(5, 5, 244, 26);
    m_entry->set_text_alignment(Gfx::TextAlignment::CenterRight);

    // FIXME: Use GML for this.
    m_label = *find_descendant_of_type_named<GUI::Label>("label");
    m_label->set_frame_style(Gfx::FrameStyle::SunkenContainer);

    for (int i = 0; i < 10; i++) {
        m_digit_button[i] = *find_descendant_of_type_named<GUI::Button>(TRY(String::formatted("{}_button", i)));
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
        m_keypad.set_to_0();
        m_calculator.clear_operation();
        update_display();
    };

    m_clear_error_button = *find_descendant_of_type_named<GUI::Button>("clear_error_button");
    m_clear_error_button->on_click = [this](auto) {
        m_keypad.set_to_0();
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
    add_operation_button(*m_equals_button, Calculator::Operation::Equals);

    return {};
}

void CalculatorWidget::perform_operation(Calculator::Operation operation)
{
    Optional<Crypto::BigFraction> res;
    if (m_keypad.in_typing_state()) {
        Crypto::BigFraction argument = m_keypad.value();
        res = m_calculator.operation_with_literal_argument(operation, move(argument));
    } else {
        res = m_calculator.operation_without_argument(operation);
    }

    if (res.has_value()) {
        m_keypad.set_value(move(res.value()));
    }
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
    return String::from_byte_string(m_entry->text()).release_value_but_fixme_should_propagate_errors();
}

void CalculatorWidget::set_entry(Crypto::BigFraction value)
{
    m_keypad.set_value(move(value));
    update_display();
}

void CalculatorWidget::set_typed_entry(Crypto::BigFraction value)
{
    m_keypad.set_typed_value(move(value));
    update_display();
}

void CalculatorWidget::update_display()
{
    m_entry->set_text(m_keypad.to_string().release_value_but_fixme_should_propagate_errors());
    if (m_calculator.has_error())
        m_label->set_text("E"_string);
    else
        m_label->set_text({});
}

void CalculatorWidget::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == KeyCode::Key_Return || event.key() == KeyCode::Key_Equal)
        m_equals_button->click();
    else if (event.code_point() >= '0' && event.code_point() <= '9')
        m_digit_button[event.code_point() - '0']->click();
    else if (event.code_point() == '.')
        m_decimal_point_button->click();
    else if (event.key() == KeyCode::Key_Escape || event.key() == KeyCode::Key_Delete)
        m_clear_button->click();
    else if (event.key() == KeyCode::Key_Backspace)
        m_backspace_button->click();
    else if (event.key() == KeyCode::Key_Backslash)
        m_sign_button->click();
    else if (event.key() == KeyCode::Key_S)
        m_sqrt_button->click();
    else if (event.key() == KeyCode::Key_Percent)
        m_percent_button->click();
    else if (event.key() == KeyCode::Key_I)
        m_inverse_button->click();
    else if (event.code_point() == '+')
        m_add_button->click();
    else if (event.code_point() == '-')
        m_subtract_button->click();
    else if (event.code_point() == '*')
        m_multiply_button->click();
    else if (event.code_point() == '/')
        m_divide_button->click();
    else if (event.code_point() == '%')
        m_percent_button->click();
    else
        event.ignore();

    update_display();
}

void CalculatorWidget::shrink(unsigned shrink_threshold)
{
    m_keypad.shrink(shrink_threshold);
    update_display();
}

unsigned CalculatorWidget::rounding_length() const
{
    return m_keypad.rounding_length();
}

void CalculatorWidget::set_rounding_length(unsigned rounding_threshold)
{
    m_keypad.set_rounding_length(rounding_threshold);
    update_display();
}

void CalculatorWidget::set_rounding_custom(GUI::Action& action, StringView format)
{
    m_format = format;
    m_rounding_custom = action;
}

}
