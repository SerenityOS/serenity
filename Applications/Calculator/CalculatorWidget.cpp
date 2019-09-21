#include "CalculatorWidget.h"
#include <AK/Assertions.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GTextBox.h>

CalculatorWidget::CalculatorWidget(GWidget* parent)
    : GWidget(parent)
{
    set_fill_with_background_color(true);

    m_entry = new GTextBox(this);
    m_entry->set_relative_rect(5, 5, 244, 26);
    m_entry->set_text_alignment(TextAlignment::CenterRight);

    m_label = GLabel::construct(this);
    m_label->set_relative_rect(12, 42, 27, 27);
    m_label->set_foreground_color(Color::NamedColor::Red);
    m_label->set_frame_shadow(FrameShadow::Sunken);
    m_label->set_frame_shape(FrameShape::Container);
    m_label->set_frame_thickness(2);

    update_display();

    for (int i = 0; i < 10; i++) {
        auto& button = *new GButton(this);
        int p = i ? i + 2 : 0;
        int x = 55 + (p % 3) * 39;
        int y = 177 - (p / 3) * 33;
        button.move_to(x, y);
        button.set_foreground_color(Color::NamedColor::Blue);
        add_button(button, i);
    }

    auto& button_mem_add = *new GButton(this);
    button_mem_add.move_to(9, 177);
    button_mem_add.set_foreground_color(Color::NamedColor::Red);
    button_mem_add.set_text("M+");
    add_button(button_mem_add, Calculator::Operation::MemAdd);

    auto& button_mem_save = *new GButton(this);
    button_mem_save.move_to(9, 144);
    button_mem_save.set_foreground_color(Color::NamedColor::Red);
    button_mem_save.set_text("MS");
    add_button(button_mem_save, Calculator::Operation::MemSave);

    auto& button_mem_recall = *new GButton(this);
    button_mem_recall.move_to(9, 111);
    button_mem_recall.set_foreground_color(Color::NamedColor::Red);
    button_mem_recall.set_text("MR");
    add_button(button_mem_recall, Calculator::Operation::MemRecall);

    auto& button_mem_clear = *new GButton(this);
    button_mem_clear.move_to(9, 78);
    button_mem_clear.set_foreground_color(Color::NamedColor::Red);
    button_mem_clear.set_text("MC");
    add_button(button_mem_clear, Calculator::Operation::MemClear);

    auto& button_clear = *new GButton(this);
    button_clear.set_foreground_color(Color::NamedColor::Red);
    button_clear.set_text("C");
    button_clear.on_click = [this](GButton&) {
        m_keypad.set_value(0.0);
        m_calculator.clear_operation();
        update_display();
    };
    add_button(button_clear);
    button_clear.set_relative_rect(187, 40, 60, 28);

    auto& button_clear_error = *new GButton(this);
    button_clear_error.set_foreground_color(Color::NamedColor::Red);
    button_clear_error.set_text("CE");
    button_clear_error.on_click = [this](GButton&) {
        m_calculator.clear_error();
        update_display();
    };
    add_button(button_clear_error);
    button_clear_error.set_relative_rect(124, 40, 59, 28);

    auto& button_backspace = *new GButton(this);
    button_backspace.set_foreground_color(Color::NamedColor::Red);
    button_backspace.set_text("Backspace");
    button_backspace.on_click = [this](GButton&) {
        m_keypad.type_backspace();
        update_display();
    };
    add_button(button_backspace);
    button_backspace.set_relative_rect(55, 40, 65, 28);

    auto& button_decimal_point = *new GButton(this);
    button_decimal_point.move_to(133, 177);
    button_decimal_point.set_foreground_color(Color::NamedColor::Blue);
    button_decimal_point.set_text(".");
    button_decimal_point.on_click = [this](GButton&) {
        m_keypad.type_decimal_point();
        update_display();
    };
    add_button(button_decimal_point);

    auto& button_toggle_sign = *new GButton(this);
    button_toggle_sign.move_to(94, 177);
    button_toggle_sign.set_foreground_color(Color::NamedColor::Blue);
    button_toggle_sign.set_text("+/-");
    add_button(button_toggle_sign, Calculator::Operation::ToggleSign);

    auto& button_add = *new GButton(this);
    button_add.move_to(172, 177);
    button_add.set_foreground_color(Color::NamedColor::Red);
    button_add.set_text("+");
    add_button(button_add, Calculator::Operation::Add);

    auto& button_subtract = *new GButton(this);
    button_subtract.move_to(172, 144);
    button_subtract.set_foreground_color(Color::NamedColor::Red);
    button_subtract.set_text("-");
    add_button(button_subtract, Calculator::Operation::Subtract);

    auto& button_multiply = *new GButton(this);
    button_multiply.move_to(172, 111);
    button_multiply.set_foreground_color(Color::NamedColor::Red);
    button_multiply.set_text("*");
    add_button(button_multiply, Calculator::Operation::Multiply);

    auto& button_divide = *new GButton(this);
    button_divide.move_to(172, 78);
    button_divide.set_foreground_color(Color::NamedColor::Red);
    button_divide.set_text("/");
    add_button(button_divide, Calculator::Operation::Divide);

    auto& button_sqrt = *new GButton(this);
    button_sqrt.move_to(211, 78);
    button_sqrt.set_foreground_color(Color::NamedColor::Blue);
    button_sqrt.set_text("sqrt");
    add_button(button_sqrt, Calculator::Operation::Sqrt);

    auto& button_inverse = *new GButton(this);
    button_inverse.move_to(211, 144);
    button_inverse.set_foreground_color(Color::NamedColor::Blue);
    button_inverse.set_text("1/x");
    add_button(button_inverse, Calculator::Operation::Inverse);

    auto& button_percent = *new GButton(this);
    button_percent.move_to(211, 111);
    button_percent.set_foreground_color(Color::NamedColor::Blue);
    button_percent.set_text("%");
    add_button(button_percent, Calculator::Operation::Percent);

    auto& button_equals = *new GButton(this);
    button_equals.move_to(211, 177);
    button_equals.set_foreground_color(Color::NamedColor::Red);
    button_equals.set_text("=");
    button_equals.on_click = [this](GButton&) {
        double argument = m_keypad.value();
        double res = m_calculator.finish_operation(argument);
        m_keypad.set_value(res);
        update_display();
    };
    add_button(button_equals);
}

CalculatorWidget::~CalculatorWidget()
{
}

void CalculatorWidget::add_button(GButton& button, Calculator::Operation operation)
{
    add_button(button);
    button.on_click = [this, operation](GButton&) {
        double argument = m_keypad.value();
        double res = m_calculator.begin_operation(operation, argument);
        m_keypad.set_value(res);
        update_display();
    };
}

void CalculatorWidget::add_button(GButton& button, int digit)
{
    add_button(button);
    button.set_text(String::number(digit));
    button.on_click = [this, digit](GButton&) {
        m_keypad.type_digit(digit);
        update_display();
    };
}

void CalculatorWidget::add_button(GButton& button)
{
    button.resize(35, 28);
}

void CalculatorWidget::update_display()
{
    m_entry->set_text(m_keypad.to_string());
    if (m_calculator.has_error())
        m_label->set_text("E");
    else
        m_label->set_text("");
}
