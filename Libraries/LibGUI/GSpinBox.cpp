#include <LibGUI/GButton.h>
#include <LibGUI/GSpinBox.h>
#include <LibGUI/GTextEditor.h>

GSpinBox::GSpinBox(GWidget* parent)
    : GWidget(parent)
{
    m_editor = new GTextEditor(GTextEditor::Type::SingleLine, this);
    m_editor->set_text("0");
    m_editor->on_change = [this] {
        bool ok;
        int value = m_editor->text().to_uint(ok);
        if (ok)
            set_value(value);
        else
            m_editor->set_text(String::number(m_value));
    };
    m_increment_button = new GButton(this);
    m_increment_button->set_focusable(false);
    m_increment_button->set_text("\xf6");
    m_increment_button->on_click = [this](GButton&) { set_value(m_value + 1); };
    m_increment_button->set_auto_repeat_interval(150);
    m_decrement_button = new GButton(this);
    m_decrement_button->set_focusable(false);
    m_decrement_button->set_text("\xf7");
    m_decrement_button->on_click = [this](GButton&) { set_value(m_value - 1); };
    m_decrement_button->set_auto_repeat_interval(150);
}

GSpinBox::~GSpinBox()
{
}

void GSpinBox::set_value(int value)
{
    if (value < m_min)
        value = m_min;
    if (value > m_max)
        value = m_max;
    if (m_value == value)
        return;
    m_value = value;
    m_editor->set_text(String::number(value));
    update();
    if (on_change)
        on_change(value);
}

void GSpinBox::set_range(int min, int max)
{
    ASSERT(min <= max);
    if (m_min == min && m_max == max)
        return;

    m_min = min;
    m_max = max;

    int old_value = m_value;
    if (m_value < m_min)
        m_value = m_min;
    if (m_value > m_max)
        m_value = m_max;
    if (on_change && m_value != old_value)
        on_change(m_value);

    update();
}

void GSpinBox::resize_event(GResizeEvent& event)
{
    int frame_thickness = m_editor->frame_thickness();
    int button_height = (event.size().height() / 2) - frame_thickness;
    int button_width = 15;
    m_increment_button->set_relative_rect(width() - button_width - frame_thickness, frame_thickness, button_width, button_height);
    m_decrement_button->set_relative_rect(width() - button_width - frame_thickness, frame_thickness + button_height, button_width, button_height);
    m_editor->set_relative_rect(0, 0, width(), height());
}
