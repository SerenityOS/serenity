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
