/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <LibCore/Timer.h>
#include <LibGUI/AbstractButton.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

namespace GUI {

AbstractButton::AbstractButton(String text)
{
    set_text(move(text));

    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    set_background_role(Gfx::ColorRole::Button);
    set_foreground_role(Gfx::ColorRole::ButtonText);

    m_auto_repeat_timer = add<Core::Timer>();
    m_auto_repeat_timer->on_timeout = [this] {
        click();
    };

    REGISTER_STRING_PROPERTY("text", text, set_text);
    REGISTER_BOOL_PROPERTY("checked", is_checked, set_checked);
    REGISTER_BOOL_PROPERTY("checkable", is_checkable, set_checkable);
    REGISTER_BOOL_PROPERTY("exclusive", is_exclusive, set_exclusive);
}

void AbstractButton::set_text(String text)
{
    if (m_text == text)
        return;
    m_text = move(text);
    update();
}

void AbstractButton::set_checked(bool checked, AllowCallback allow_callback)
{
    if (m_checked == checked)
        return;
    m_checked = checked;

    if (is_exclusive() && checked && parent_widget()) {
        bool sibling_had_focus = false;
        parent_widget()->for_each_child_of_type<AbstractButton>([&](auto& sibling) {
            if (!sibling.is_exclusive())
                return IterationDecision::Continue;
            if (window() && window()->focused_widget() == &sibling)
                sibling_had_focus = true;
            if (!sibling.is_checked())
                return IterationDecision::Continue;
            sibling.m_checked = false;
            sibling.update();
            if (sibling.on_checked)
                sibling.on_checked(false);
            return IterationDecision::Continue;
        });
        m_checked = true;
        if (sibling_had_focus)
            set_focus(true);
    }

    if (is_exclusive() && parent_widget()) {
        // In a group of exclusive checkable buttons, only the currently checked button is focusable.
        parent_widget()->for_each_child_of_type<GUI::AbstractButton>([&](auto& button) {
            if (button.is_exclusive() && button.is_checkable())
                button.set_focus_policy(button.is_checked() ? GUI::FocusPolicy::StrongFocus : GUI::FocusPolicy::NoFocus);
            return IterationDecision::Continue;
        });
    }

    update();
    if (on_checked && allow_callback == AllowCallback::Yes)
        on_checked(checked);
}

void AbstractButton::set_checkable(bool checkable)
{
    if (m_checkable == checkable)
        return;
    m_checkable = checkable;
    update();
}

void AbstractButton::mousemove_event(MouseEvent& event)
{
    bool is_over = rect().contains(event.position());
    m_hovered = is_over;
    if (event.buttons() & m_pressed_mouse_button) {
        bool being_pressed = is_over;
        if (being_pressed != m_being_pressed) {
            m_being_pressed = being_pressed;
            if (m_auto_repeat_interval) {
                if (!m_being_pressed)
                    m_auto_repeat_timer->stop();
                else
                    m_auto_repeat_timer->start(m_auto_repeat_interval);
            }
            update();
        }
    }
    Widget::mousemove_event(event);
}

void AbstractButton::mousedown_event(MouseEvent& event)
{
    if (event.button() & m_allowed_mouse_buttons_for_pressing) {
        m_being_pressed = true;
        m_pressed_mouse_button = event.button();
        repaint();

        if (m_auto_repeat_interval) {
            click();
            m_auto_repeat_timer->start(m_auto_repeat_interval);
        }
        event.accept();
    }
    Widget::mousedown_event(event);
}

void AbstractButton::mouseup_event(MouseEvent& event)
{
    if (event.button() == m_pressed_mouse_button && m_being_pressed) {
        bool was_auto_repeating = m_auto_repeat_timer->is_active();
        m_auto_repeat_timer->stop();
        m_was_being_pressed = m_being_pressed;
        ScopeGuard update_was_being_pressed { [this] { m_was_being_pressed = m_being_pressed; } };
        m_being_pressed = false;
        m_pressed_mouse_button = MouseButton::None;
        if (!is_checkable() || is_checked())
            repaint();
        if (m_was_being_pressed && !was_auto_repeating) {
            switch (event.button()) {
            case MouseButton::Primary:
                click(event.modifiers());
                break;
            case MouseButton::Middle:
                middle_mouse_click(event.modifiers());
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        }
    }
    Widget::mouseup_event(event);
}

void AbstractButton::doubleclick_event(GUI::MouseEvent& event)
{
    double_click(event.modifiers());
    Widget::doubleclick_event(event);
}

void AbstractButton::enter_event(Core::Event&)
{
    m_hovered = true;
    update();
}

void AbstractButton::leave_event(Core::Event& event)
{
    m_hovered = false;
    if (m_being_keyboard_pressed)
        m_being_keyboard_pressed = m_being_pressed = false;
    update();
    event.accept();
    Widget::leave_event(event);
}

void AbstractButton::focusout_event(GUI::FocusEvent& event)
{
    if (m_being_keyboard_pressed) {
        m_being_pressed = m_being_keyboard_pressed = false;
        event.accept();
        update();
    }
    Widget::focusout_event(event);
}

void AbstractButton::keydown_event(KeyEvent& event)
{
    if (event.key() == KeyCode::Key_Return || event.key() == KeyCode::Key_Space) {
        m_being_pressed = m_being_keyboard_pressed = true;
        update();
        event.accept();
        return;
    } else if (m_being_pressed && event.key() == KeyCode::Key_Escape) {
        m_being_pressed = m_being_keyboard_pressed = false;
        update();
        event.accept();
        return;
    }

    // Arrow keys switch the currently checked option within an exclusive group of checkable buttons.
    if (event.is_arrow_key() && !event.modifiers() && is_exclusive() && is_checkable() && parent_widget()) {
        event.accept();
        Vector<GUI::AbstractButton&> exclusive_siblings;
        size_t this_index = 0;
        parent_widget()->for_each_child_of_type<GUI::AbstractButton>([&](auto& sibling) {
            if (&sibling == this) {
                VERIFY(is_enabled());
                this_index = exclusive_siblings.size();
            }
            if (sibling.is_exclusive() && sibling.is_checkable() && sibling.is_enabled())
                exclusive_siblings.append(sibling);
            return IterationDecision::Continue;
        });
        if (exclusive_siblings.size() <= 1)
            return;
        size_t new_checked_index;
        if (event.key() == KeyCode::Key_Left || event.key() == KeyCode::Key_Up)
            new_checked_index = this_index == 0 ? exclusive_siblings.size() - 1 : this_index - 1;
        else
            new_checked_index = this_index == exclusive_siblings.size() - 1 ? 0 : this_index + 1;
        exclusive_siblings[new_checked_index].click();
        return;
    }
    Widget::keydown_event(event);
}

void AbstractButton::keyup_event(KeyEvent& event)
{
    bool was_being_pressed = m_being_pressed;
    if (was_being_pressed && (event.key() == KeyCode::Key_Return || event.key() == KeyCode::Key_Space)) {
        m_being_pressed = m_being_keyboard_pressed = false;
        click(event.modifiers());
        update();
        event.accept();
        return;
    }
    Widget::keyup_event(event);
}

void AbstractButton::paint_text(Painter& painter, Gfx::IntRect const& rect, Gfx::Font const& font, Gfx::TextAlignment text_alignment, Gfx::TextWrapping text_wrapping)
{
    auto clipped_rect = rect.intersected(this->rect());

    if (!is_enabled()) {
        painter.draw_text(clipped_rect.translated(1, 1), text(), font, text_alignment, palette().disabled_text_back(), Gfx::TextElision::Right, text_wrapping);
        painter.draw_text(clipped_rect, text(), font, text_alignment, palette().disabled_text_front(), Gfx::TextElision::Right, text_wrapping);
        return;
    }

    if (text().is_empty())
        return;
    painter.draw_text(clipped_rect, text(), font, text_alignment, palette().color(foreground_role()), Gfx::TextElision::Right, text_wrapping);
}

void AbstractButton::change_event(Event& event)
{
    if (event.type() == Event::Type::EnabledChange) {
        if (m_auto_repeat_timer->is_active())
            m_auto_repeat_timer->stop();
        if (!is_enabled()) {
            bool was_being_pressed = m_being_pressed;
            m_being_pressed = false;
            if (was_being_pressed)
                update();
        }
    }
    Widget::change_event(event);
}

}
