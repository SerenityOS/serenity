/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/AbstractButton.h>
#include <LibGUI/Action.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/StylePainter.h>
#include <LibGfx/TextAlignment.h>

namespace GUI {

class Button : public AbstractButton {
    C_OBJECT(Button);

public:
    virtual ~Button() override;

    void set_icon(RefPtr<Gfx::Bitmap>);
    void set_icon_from_path(String const&);
    Gfx::Bitmap const* icon() const { return m_icon.ptr(); }
    Gfx::Bitmap* icon() { return m_icon.ptr(); }

    void set_text_alignment(Gfx::TextAlignment text_alignment) { m_text_alignment = text_alignment; }
    Gfx::TextAlignment text_alignment() const { return m_text_alignment; }

    Function<void(unsigned modifiers)> on_click;
    Function<void(ContextMenuEvent&)> on_context_menu_request;

    void set_button_style(Gfx::ButtonStyle style) { m_button_style = style; }
    Gfx::ButtonStyle button_style() const { return m_button_style; }

    virtual void click(unsigned modifiers = 0) override;
    virtual void context_menu_event(ContextMenuEvent&) override;

    Action* action() { return m_action; }
    Action const* action() const { return m_action; }
    void set_action(Action&);

    virtual bool is_uncheckable() const override;

    int icon_spacing() const { return m_icon_spacing; }
    void set_icon_spacing(int spacing) { m_icon_spacing = spacing; }

    void set_menu(RefPtr<GUI::Menu>);

    bool is_default() const;
    void set_default(bool);

    bool another_button_has_focus() const { return m_another_button_has_focus; }

    void set_mimic_pressed(bool mimic_pressed);
    bool is_mimic_pressed() const { return m_mimic_pressed; };

protected:
    explicit Button(String text = {});
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void paint_event(PaintEvent&) override;

private:
    virtual void timer_event(Core::TimerEvent&) override;

    RefPtr<Gfx::Bitmap> m_icon;
    RefPtr<GUI::Menu> m_menu;
    Gfx::ButtonStyle m_button_style { Gfx::ButtonStyle::Normal };
    Gfx::TextAlignment m_text_alignment { Gfx::TextAlignment::Center };
    WeakPtr<Action> m_action;
    int m_icon_spacing { 4 };
    bool m_another_button_has_focus { false };
    bool m_mimic_pressed { false };
};

}
