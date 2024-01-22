/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Glenford Williams <gw_dev@outlook.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Calculator.h"
#include "Keypad.h"
#include <AK/Vector.h>
#include <LibCrypto/BigFraction/BigFraction.h>
#include <LibGUI/Action.h>
#include <LibGUI/Widget.h>

namespace Calculator {

class CalculatorWidget final : public GUI::Widget {
    C_OBJECT(CalculatorWidget)
public:
    static ErrorOr<NonnullRefPtr<CalculatorWidget>> try_create();
    ErrorOr<void> initialize();

    virtual ~CalculatorWidget() override = default;
    String get_entry();
    void set_entry(Crypto::BigFraction);
    void set_typed_entry(Crypto::BigFraction);

    void shrink(unsigned);
    unsigned rounding_length() const;
    void set_rounding_length(unsigned);

    void set_rounding_custom(GUI::Action& action, StringView);

private:
    CalculatorWidget() = default;

    void add_operation_button(GUI::Button&, Calculator::Operation);
    void add_digit_button(GUI::Button&, int digit);

    void perform_operation(Calculator::Operation operation);
    void update_display();

    virtual void keydown_event(GUI::KeyEvent&) override;

    Calculator m_calculator;
    Keypad m_keypad;

    RefPtr<GUI::TextBox> m_entry;
    RefPtr<GUI::Label> m_label;

    RefPtr<GUI::Button> m_digit_button[10];
    RefPtr<GUI::Button> m_mem_add_button;
    RefPtr<GUI::Button> m_mem_save_button;
    RefPtr<GUI::Button> m_mem_recall_button;
    RefPtr<GUI::Button> m_mem_clear_button;
    RefPtr<GUI::Button> m_clear_button;
    RefPtr<GUI::Button> m_clear_error_button;
    RefPtr<GUI::Button> m_backspace_button;
    RefPtr<GUI::Button> m_decimal_point_button;
    RefPtr<GUI::Button> m_sign_button;
    RefPtr<GUI::Button> m_add_button;
    RefPtr<GUI::Button> m_subtract_button;
    RefPtr<GUI::Button> m_multiply_button;
    RefPtr<GUI::Button> m_divide_button;
    RefPtr<GUI::Button> m_sqrt_button;
    RefPtr<GUI::Button> m_inverse_button;
    RefPtr<GUI::Button> m_percent_button;
    RefPtr<GUI::Button> m_equals_button;

    StringView m_format;
    RefPtr<GUI::Action> m_rounding_custom;
};

}
