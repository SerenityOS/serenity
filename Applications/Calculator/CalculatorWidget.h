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

#pragma once

#include "Calculator.h"
#include "Keypad.h"
#include <AK/Vector.h>
#include <LibGUI/Widget.h>

class CalculatorWidget final : public GUI::Widget {
    C_OBJECT(CalculatorWidget)
public:
    virtual ~CalculatorWidget() override;

private:
    explicit CalculatorWidget(GUI::Widget*);
    void add_button(GUI::Button&, Calculator::Operation);
    void add_button(GUI::Button&, int);
    void add_button(GUI::Button&);

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
};
