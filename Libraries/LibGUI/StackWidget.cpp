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

#include <LibGUI/BoxLayout.h>
#include <LibGUI/StackWidget.h>

namespace GUI {

StackWidget::StackWidget()
{
}

StackWidget::~StackWidget()
{
}

void StackWidget::set_active_widget(Widget* widget)
{
    if (widget == m_active_widget)
        return;

    bool had_focus = is_focused() || (m_active_widget && m_active_widget->is_focused());

    if (m_active_widget)
        m_active_widget->set_visible(false);
    m_active_widget = widget;
    if (m_active_widget) {
        m_active_widget->set_relative_rect(rect());
        if (had_focus)
            m_active_widget->set_focus(true);
        m_active_widget->set_visible(true);
    }
    if (on_active_widget_change)
        on_active_widget_change(m_active_widget);
}

void StackWidget::resize_event(ResizeEvent& event)
{
    if (!m_active_widget)
        return;
    m_active_widget->set_relative_rect({ {}, event.size() });
}

void StackWidget::child_event(Core::ChildEvent& event)
{
    if (!event.child() || !is<Widget>(*event.child()))
        return Widget::child_event(event);
    auto& child = downcast<Widget>(*event.child());
    if (event.type() == Event::ChildAdded) {
        if (!m_active_widget)
            set_active_widget(&child);
        else if (m_active_widget != &child)
            child.set_visible(false);
    } else if (event.type() == Event::ChildRemoved) {
        if (m_active_widget == &child) {
            Widget* new_active_widget = nullptr;
            for_each_child_widget([&](auto& new_child) {
                new_active_widget = &new_child;
                return IterationDecision::Break;
            });
            set_active_widget(new_active_widget);
        }
    }
    Widget::child_event(event);
}

}
