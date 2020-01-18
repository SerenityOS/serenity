/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibGUI/GWidget.h>

class GTextBox;
class GButton;
class GLabel;

class CalculatorWidget final : public GWidget {
    C_OBJECT(CalculatorWidget)
public:
    virtual ~CalculatorWidget() override;

private:
    explicit CalculatorWidget(GWidget*);
    void add_button(GButton&, Calculator::Operation);
    void add_button(GButton&, int);
    void add_button(GButton&);

    void update_display();

    virtual void keydown_event(GKeyEvent&) override;

    Calculator m_calculator;
    Keypad m_keypad;

    RefPtr<GTextBox> m_entry;
    RefPtr<GLabel> m_label;

    RefPtr<GButton> m_digit_button[10];
    RefPtr<GButton> m_mem_add_button;
    RefPtr<GButton> m_mem_save_button;
    RefPtr<GButton> m_mem_recall_button;
    RefPtr<GButton> m_mem_clear_button;
    RefPtr<GButton> m_clear_button;
    RefPtr<GButton> m_clear_error_button;
    RefPtr<GButton> m_backspace_button;
    RefPtr<GButton> m_decimal_point_button;
    RefPtr<GButton> m_sign_button;
    RefPtr<GButton> m_add_button;
    RefPtr<GButton> m_subtract_button;
    RefPtr<GButton> m_multiply_button;
    RefPtr<GButton> m_divide_button;
    RefPtr<GButton> m_sqrt_button;
    RefPtr<GButton> m_inverse_button;
    RefPtr<GButton> m_percent_button;
    RefPtr<GButton> m_equals_button;
};
