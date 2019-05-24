#pragma once

#include <LibGUI/GWidget.h>

class GAbstractButton : public GWidget {
public:
    virtual ~GAbstractButton() override;

    void set_text(const String&);
    const String& text() const { return m_text; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool);

    bool is_hovered() const { return m_hovered; }
    bool is_being_pressed() const { return m_being_pressed; }

    virtual void click() = 0;

    virtual const char* class_name() const override { return "GAbstractButton"; }

protected:
    explicit GAbstractButton(GWidget* parent);
    GAbstractButton(const String&, GWidget* parent);

    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void enter_event(CEvent&) override;
    virtual void leave_event(CEvent&) override;

private:
    String m_text;
    bool m_checked { false };
    bool m_checkable { false };
    bool m_hovered { false };
    bool m_being_pressed { false };
};
