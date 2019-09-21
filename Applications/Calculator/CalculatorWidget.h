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

    Calculator m_calculator;
    Keypad m_keypad;

    ObjectPtr<GTextBox> m_entry;
    ObjectPtr<GLabel> m_label;

    ObjectPtr<GButton> m_digit_button[10];
    ObjectPtr<GButton> m_mem_add_button;
    ObjectPtr<GButton> m_mem_save_button;
    ObjectPtr<GButton> m_mem_recall_button;
    ObjectPtr<GButton> m_mem_clear_button;
    ObjectPtr<GButton> m_clear_button;
    ObjectPtr<GButton> m_clear_error_button;
    ObjectPtr<GButton> m_backspace_button;
    ObjectPtr<GButton> m_decimal_point_button;
    ObjectPtr<GButton> m_sign_button;
    ObjectPtr<GButton> m_add_button;
    ObjectPtr<GButton> m_subtract_button;
    ObjectPtr<GButton> m_multiply_button;
    ObjectPtr<GButton> m_divide_button;
    ObjectPtr<GButton> m_sqrt_button;
    ObjectPtr<GButton> m_inverse_button;
    ObjectPtr<GButton> m_percent_button;
    ObjectPtr<GButton> m_equals_button;

};
