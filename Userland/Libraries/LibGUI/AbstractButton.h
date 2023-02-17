/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGUI/Widget.h>
#include <LibGfx/TextWrapping.h>

namespace GUI {

class AbstractButton : public Widget {
    C_OBJECT_ABSTRACT(AbstractButton);

public:
    virtual ~AbstractButton() override = default;

    Function<void(bool)> on_checked;

    virtual void set_text(String);
    String const& text() const { return m_text; }

    bool is_exclusive() const { return m_exclusive; }
    void set_exclusive(bool b) { m_exclusive = b; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool, AllowCallback = AllowCallback::Yes);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool);

    bool is_hovered() const { return m_hovered; }
    bool is_being_pressed() const { return m_being_pressed; }
    bool was_being_pressed() const { return m_was_being_pressed; }

    unsigned allowed_mouse_buttons_for_pressing() const { return m_allowed_mouse_buttons_for_pressing; }
    void set_allowed_mouse_buttons_for_pressing(unsigned allowed_buttons) { m_allowed_mouse_buttons_for_pressing = allowed_buttons; }

    virtual void click(unsigned modifiers = 0) = 0;
    virtual void double_click(unsigned) { }
    virtual void middle_mouse_click(unsigned) { }
    virtual bool is_uncheckable() const { return true; }

    int auto_repeat_interval() const { return m_auto_repeat_interval; }
    void set_auto_repeat_interval(int interval) { m_auto_repeat_interval = interval; }

protected:
    explicit AbstractButton(String = {});

    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void keyup_event(KeyEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void focusout_event(GUI::FocusEvent&) override;
    virtual void change_event(Event&) override;

    void paint_text(Painter&, Gfx::IntRect const&, Gfx::Font const&, Gfx::TextAlignment, Gfx::TextWrapping = Gfx::TextWrapping::DontWrap);

private:
    String m_text;
    bool m_checked { false };
    bool m_checkable { false };
    bool m_hovered { false };
    bool m_being_pressed { false };
    bool m_was_being_pressed { false };
    bool m_being_keyboard_pressed { false };
    bool m_exclusive { false };

    MouseButton m_pressed_mouse_button { MouseButton::None };
    unsigned m_allowed_mouse_buttons_for_pressing { MouseButton::Primary };

    int m_auto_repeat_interval { 0 };
    RefPtr<Core::Timer> m_auto_repeat_timer;
};

}
