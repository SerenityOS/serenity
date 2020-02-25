/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibCore/Timer.h>
#include <LibGUI/AbstractButton.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

namespace GUI {

AbstractButton::AbstractButton(const StringView& text)
    : m_text(text)
{
    m_auto_repeat_timer = add<Core::Timer>();
    m_auto_repeat_timer->on_timeout = [this] {
        click();
    };
}

AbstractButton::~AbstractButton()
{
}

void AbstractButton::set_text(const StringView& text)
{
    if (m_text == text)
        return;
    m_text = text;
    update();
}

void AbstractButton::set_checked(bool checked)
{
    if (m_checked == checked)
        return;
    m_checked = checked;

    if (is_exclusive() && checked) {
        parent_widget()->for_each_child_of_type<AbstractButton>([&](auto& sibling) {
            if (!sibling.is_exclusive() || !sibling.is_checked())
                return IterationDecision::Continue;
            sibling.m_checked = false;
            sibling.update();
            if (sibling.on_checked)
                sibling.on_checked(false);
            return IterationDecision::Continue;
        });
        m_checked = true;
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
        if (is_enabled()) {
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
    }
    Widget::mousemove_event(event);
}

void AbstractButton::mousedown_event(MouseEvent& event)
{
#ifdef GABSTRACTBUTTON_DEBUG
    dbgprintf("AbstractButton::mouse_down_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
#endif
    if (event.button() == MouseButton::Left) {
        if (is_enabled()) {
            m_being_pressed = true;
            update();

            if (m_auto_repeat_interval) {
                click();
                m_auto_repeat_timer->start(m_auto_repeat_interval);
            }
        }
    }
    Widget::mousedown_event(event);
}

void AbstractButton::mouseup_event(MouseEvent& event)
{
#ifdef GABSTRACTBUTTON_DEBUG
    dbgprintf("AbstractButton::mouse_up_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
#endif
    if (event.button() == MouseButton::Left) {
        bool was_auto_repeating = m_auto_repeat_timer->is_active();
        m_auto_repeat_timer->stop();
        if (is_enabled()) {
            bool was_being_pressed = m_being_pressed;
            m_being_pressed = false;
            update();
            if (was_being_pressed && !was_auto_repeating)
                click();
        }
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
    if (event.key() == KeyCode::Key_Return) {
        click();
        event.accept();
        return;
    }
    Widget::keydown_event(event);
}

void AbstractButton::paint_text(Painter& painter, const Gfx::Rect& rect, const Gfx::Font& font, Gfx::TextAlignment text_alignment)
{
    auto clipped_rect = rect.intersected(this->rect());

    if (!is_enabled()) {
        painter.draw_text(clipped_rect.translated(1, 1), text(), font, text_alignment, Color::White, Gfx::TextElision::Right);
        painter.draw_text(clipped_rect, text(), font, text_alignment, Color::from_rgb(0x808080), Gfx::TextElision::Right);
        return;
    }

    if (text().is_empty())
        return;
    painter.draw_text(clipped_rect, text(), font, text_alignment, palette().button_text(), Gfx::TextElision::Right);
    if (is_focused())
        painter.draw_rect(clipped_rect.inflated(6, 4), palette().focus_outline());
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
