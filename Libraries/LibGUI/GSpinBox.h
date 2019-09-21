#pragma once

#include <LibGUI/GWidget.h>

class GButton;
class GTextEditor;

class GSpinBox : public GWidget {
    C_OBJECT(GSpinBox)
public:
    virtual ~GSpinBox() override;

    int value() const { return m_value; }
    void set_value(int);

    int min() const { return m_min; }
    int max() const { return m_max; }
    void set_min(int min) { set_range(min, max()); }
    void set_max(int max) { set_range(min(), max); }
    void set_range(int min, int max);

    Function<void(int value)> on_change;

protected:
    explicit GSpinBox(GWidget* parent = nullptr);

    virtual void resize_event(GResizeEvent&) override;

private:
    RefPtr<GTextEditor> m_editor;
    RefPtr<GButton> m_increment_button;
    RefPtr<GButton> m_decrement_button;

    int m_min { 0 };
    int m_max { 100 };
    int m_value { 0 };
};
