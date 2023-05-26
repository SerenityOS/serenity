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

ErrorOr<NonnullRefPtr<CalculatorWidget>> CalculatorWidget::create()
{
    auto widget = TRY(CalculatorWidget::try_create());

    widget->m_entry = *widget->find_descendant_of_type_named<GUI::TextBox>("entry_textbox");
    // FIXME: Use GML for this.
    widget->m_entry->set_relative_rect(5, 5, 244, 26);
    widget->m_entry->set_text_alignment(Gfx::TextAlignment::CenterRight);

    // FIXME: Use GML for this.
    widget->m_label = *widget->find_descendant_of_type_named<GUI::Label>("label");
    widget->m_label->set_frame_style(Gfx::FrameStyle::SunkenContainer);

    for (int i = 0; i < 10; i++) {
        widget->m_digit_button[i] = *widget->find_descendant_of_type_named<GUI::Button>(TRY(String::formatted("{}_button", i)));
        widget->add_digit_button(*widget->m_digit_button[i], i);
    }

    widget->m_mem_add_button = *widget->find_descendant_of_type_named<GUI::Button>("mem_add_button");
    widget->add_operation_button(*widget->m_mem_add_button, Calculator::Operation::MemAdd);

    widget->m_mem_save_button = *widget->find_descendant_of_type_named<GUI::Button>("mem_save_button");
    widget->add_operation_button(*widget->m_mem_save_button, Calculator::Operation::MemSave);

    widget->m_mem_recall_button = *widget->find_descendant_of_type_named<GUI::Button>("mem_recall_button");
    widget->add_operation_button(*widget->m_mem_recall_button, Calculator::Operation::MemRecall);

    widget->m_mem_clear_button = *widget->find_descendant_of_type_named<GUI::Button>("mem_clear_button");
    widget->add_operation_button(*widget->m_mem_clear_button, Calculator::Operation::MemClear);

    widget->m_clear_button = *widget->find_descendant_of_type_named<GUI::Button>("clear_button");
    widget->m_clear_button->on_click = [self = NonnullRefPtr<CalculatorWidget>(widget)](auto) {
        self->m_keypad.set_to_0();
        self->m_calculator.clear_operation();
        self->update_display();
    };

    widget->m_clear_error_button = *widget->find_descendant_of_type_named<GUI::Button>("clear_error_button");
    widget->m_clear_error_button->on_click = [self = NonnullRefPtr<CalculatorWidget>(widget)](auto) {
        self->m_keypad.set_to_0();
        self->update_display();
    };

    widget->m_backspace_button = *widget->find_descendant_of_type_named<GUI::Button>("backspace_button");
    widget->m_backspace_button->on_click = [self = NonnullRefPtr<CalculatorWidget>(widget)](auto) {
        self->m_keypad.type_backspace();
        self->update_display();
    };

    widget->m_decimal_point_button = *widget->find_descendant_of_type_named<GUI::Button>("decimal_button");
    widget->m_decimal_point_button->on_click = [self = NonnullRefPtr<CalculatorWidget>(widget)](auto) {
        self->m_keypad.type_decimal_point();
        self->update_display();
    };

    widget->m_sign_button = *widget->find_descendant_of_type_named<GUI::Button>("sign_button");
    widget->add_operation_button(*widget->m_sign_button, Calculator::Operation::ToggleSign);

    widget->m_add_button = *widget->find_descendant_of_type_named<GUI::Button>("add_button");
    widget->add_operation_button(*widget->m_add_button, Calculator::Operation::Add);

    widget->m_subtract_button = *widget->find_descendant_of_type_named<GUI::Button>("subtract_button");
    widget->add_operation_button(*widget->m_subtract_button, Calculator::Operation::Subtract);

    widget->m_multiply_button = *widget->find_descendant_of_type_named<GUI::Button>("multiply_button");
    widget->add_operation_button(*widget->m_multiply_button, Calculator::Operation::Multiply);

    widget->m_divide_button = *widget->find_descendant_of_type_named<GUI::Button>("divide_button");
    widget->add_operation_button(*widget->m_divide_button, Calculator::Operation::Divide);

    widget->m_sqrt_button = *widget->find_descendant_of_type_named<GUI::Button>("sqrt_button");
    widget->add_operation_button(*widget->m_sqrt_button, Calculator::Operation::Sqrt);

    widget->m_inverse_button = *widget->find_descendant_of_type_named<GUI::Button>("inverse_button");
    widget->add_operation_button(*widget->m_inverse_button, Calculator::Operation::Inverse);

    widget->m_percent_button = *widget->find_descendant_of_type_named<GUI::Button>("mod_button");
    widget->add_operation_button(*widget->m_percent_button, Calculator::Operation::Percent);

    widget->m_equals_button = *widget->find_descendant_of_type_named<GUI::Button>("equal_button");
    widget->add_operation_button(*widget->m_equals_button, Calculator::Operation::Equals);

    return widget;
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
