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
    explicit CalculatorWidget(GWidget*);
    virtual ~CalculatorWidget();

private:
    void add_button(GButton&, Calculator::Operation);
    void add_button(GButton&, int);
    void add_button(GButton&);

    void update_display();

    Calculator m_calculator;
    Keypad m_keypad;

    ObjectPtr<GTextBox> m_entry;
    ObjectPtr<GLabel> m_label;
};
