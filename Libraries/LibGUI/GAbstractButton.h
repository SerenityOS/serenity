#pragma once

#include <LibGUI/GWidget.h>
#include <SharedGraphics/TextAlignment.h>

class GPainter;

class GAbstractButton : public GWidget {
public:
    virtual ~GAbstractButton() override;

    Function<void(bool)> on_checked;

    void set_text(const StringView&);
    const String& text() const { return m_text; }

    bool is_exclusive() const { return m_exclusive; }
    void set_exclusive(bool b) { m_exclusive = b; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool);

    bool is_hovered() const { return m_hovered; }
    bool is_being_pressed() const { return m_being_pressed; }

    virtual void click() = 0;
    virtual const char* class_name() const override { return "GAbstractButton"; }
    virtual bool accepts_focus() const override { return true; }
    virtual bool supports_keyboard_activation() const override { return true; }
    virtual bool is_uncheckable() const { return true; }

protected:
    explicit GAbstractButton(GWidget* parent);
    GAbstractButton(const StringView&, GWidget* parent);

    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void enter_event(CEvent&) override;
    virtual void leave_event(CEvent&) override;
    virtual void change_event(GEvent&) override;

    void paint_text(GPainter&, const Rect&, const Font&, TextAlignment);

private:
    virtual bool is_abstract_button() const final { return true; }

    String m_text;
    bool m_checked { false };
    bool m_checkable { false };
    bool m_hovered { false };
    bool m_being_pressed { false };
    bool m_exclusive { false };
};

template<>
inline bool is<GAbstractButton>(const CObject& object)
{
    if (!is<GWidget>(object))
        return false;
    return to<GWidget>(object).is_abstract_button();
}
