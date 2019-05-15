#pragma once

#include <LibGUI/GWidget.h>
#include <SharedGraphics/StylePainter.h>
#include <AK/AKString.h>
#include <AK/Function.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/TextAlignment.h>

class GAction;

class GButton : public GWidget {
public:
    explicit GButton(GWidget* parent);
    virtual ~GButton() override;

    String caption() const { return m_caption; }
    void set_caption(const String&);

    void set_icon(RetainPtr<GraphicsBitmap>&&);
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }
    GraphicsBitmap* icon() { return m_icon.ptr(); }

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    void set_text_alignment(TextAlignment text_alignment) { m_text_alignment = text_alignment; }
    TextAlignment text_alignment() const { return m_text_alignment; }

    Function<void(GButton&)> on_click;

    void set_button_style(ButtonStyle style) { m_button_style = style; }
    ButtonStyle button_style() const { return m_button_style; }

    void click();

    void set_action(GAction&);

    virtual const char* class_name() const override { return "GButton"; }
    virtual bool accepts_focus() const override { return true; }

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void enter_event(CEvent&) override;
    virtual void leave_event(CEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;

private:
    String m_caption;
    RetainPtr<GraphicsBitmap> m_icon;
    ButtonStyle m_button_style { ButtonStyle::Normal };
    TextAlignment m_text_alignment { TextAlignment::Center };
    WeakPtr<GAction> m_action;
    bool m_being_pressed { false };
    bool m_hovered { false };
    bool m_checkable { false };
    bool m_checked { false };
};

