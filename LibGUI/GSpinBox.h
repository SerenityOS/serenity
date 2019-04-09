#pragma once

#include <LibGUI/GWidget.h>

class GButton;
class GTextEditor;

class GSpinBox : public GWidget {
public:
    GSpinBox(GWidget* parent = nullptr);
    virtual ~GSpinBox() override;

    int value() const { return m_value; }
    void set_value(int);

    void set_range(int min, int max);

    Function<void(int value)> on_change;

protected:
    virtual void resize_event(GResizeEvent&) override;

private:
    GTextEditor* m_editor { nullptr };
    GButton* m_increment_button { nullptr };
    GButton* m_decrement_button { nullptr };

    int m_min { 0 };
    int m_max { 100 };
    int m_value { 0 };
};
