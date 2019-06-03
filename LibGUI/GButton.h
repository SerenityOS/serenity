#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <LibGUI/GAbstractButton.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/StylePainter.h>
#include <SharedGraphics/TextAlignment.h>

class GAction;

class GButton : public GAbstractButton {
public:
    GButton(const String& text, GWidget* parent);
    explicit GButton(GWidget* parent);
    virtual ~GButton() override;

    void set_icon(RetainPtr<GraphicsBitmap>&&);
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }
    GraphicsBitmap* icon() { return m_icon.ptr(); }

    void set_text_alignment(TextAlignment text_alignment) { m_text_alignment = text_alignment; }
    TextAlignment text_alignment() const { return m_text_alignment; }

    Function<void(GButton&)> on_click;

    void set_button_style(ButtonStyle style) { m_button_style = style; }
    ButtonStyle button_style() const { return m_button_style; }

    virtual void click() override;

    void set_action(GAction&);

    virtual const char* class_name() const override { return "GButton"; }
    virtual bool accepts_focus() const override { return true; }
    virtual bool accepts_keyboard_select() const;

protected:
    virtual void paint_event(GPaintEvent&) override;

private:
    RetainPtr<GraphicsBitmap> m_icon;
    ButtonStyle m_button_style { ButtonStyle::Normal };
    TextAlignment m_text_alignment { TextAlignment::Center };
    WeakPtr<GAction> m_action;
};
