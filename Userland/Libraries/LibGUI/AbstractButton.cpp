/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

AbstractButton::~AbstractButton()
{
}

void AbstractButton::set_text(String text)
{
    if (m_text == text)
        return;
    m_text = move(text);
    update();
}

void AbstractButton::set_checked(bool checked)
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

    update();
    if (on_checked)
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
    if (event.buttons() & MouseButton::Left) {
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
    if (event.button() == MouseButton::Left) {
        m_being_pressed = true;
        update();

        if (m_auto_repeat_interval) {
            click();
            m_auto_repeat_timer->start(m_auto_repeat_interval);
        }
    }
    Widget::mousedown_event(event);
}

void AbstractButton::mouseup_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Left) {
        bool was_auto_repeating = m_auto_repeat_timer->is_active();
        m_auto_repeat_timer->stop();
        bool was_being_pressed = m_being_pressed;
        m_being_pressed = false;
        update();
        if (was_being_pressed && !was_auto_repeating)
            click(event.modifiers());
    }
    Widget::mouseup_event(event);
}

void AbstractButton::enter_event(Core::Event&)
{
    m_hovered = true;
    update();
}

void AbstractButton::leave_event(Core::Event&)
{
    m_hovered = false;
    update();
}

void AbstractButton::keydown_event(KeyEvent& event)
{
    if (event.key() == KeyCode::Key_Return || event.key() == KeyCode::Key_Space) {
        m_being_pressed = true;
        update();
        event.accept();
        return;
    } else if (m_being_pressed && event.key() == KeyCode::Key_Escape) {
        m_being_pressed = false;
        update();
        event.accept();
        return;
    }
    Widget::keydown_event(event);
}

void AbstractButton::keyup_event(KeyEvent& event)
{
    if (m_being_pressed && (event.key() == KeyCode::Key_Return || event.key() == KeyCode::Key_Space)) {
        click(event.modifiers());
        event.accept();
        return;
    }
    Widget::keyup_event(event);
}

void AbstractButton::paint_text(Painter& painter, const Gfx::IntRect& rect, const Gfx::Font& font, Gfx::TextAlignment text_alignment)
{
    auto clipped_rect = rect.intersected(this->rect());

    if (!is_enabled()) {
        painter.draw_text(clipped_rect.translated(1, 1), text(), font, text_alignment, Color::White, Gfx::TextElision::Right);
        painter.draw_text(clipped_rect, text(), font, text_alignment, Color::from_rgb(0x808080), Gfx::TextElision::Right);
        return;
    }

    if (text().is_empty())
        return;
    painter.draw_text(clipped_rect, text(), font, text_alignment, palette().color(foreground_role()), Gfx::TextElision::Right);
}

void AbstractButton::change_event(Event& event)
{
    if (event.type() == Event::Type::EnabledChange) {
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
