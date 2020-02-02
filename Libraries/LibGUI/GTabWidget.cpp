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

#include <LibDraw/Palette.h>
#include <LibDraw/StylePainter.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GTabWidget.h>

namespace GUI {

TabWidget::TabWidget(Widget* parent)
    : Widget(parent)
{
}

TabWidget::~TabWidget()
{
}

void TabWidget::add_widget(const StringView& title, Widget* widget)
{
    m_tabs.append({ title, widget });
    add_child(*widget);
}

void TabWidget::set_active_widget(Widget* widget)
{
    if (widget == m_active_widget)
        return;

    if (m_active_widget)
        m_active_widget->set_visible(false);
    m_active_widget = widget;
    if (m_active_widget) {
        m_active_widget->set_relative_rect(child_rect_for_size(size()));
        m_active_widget->set_visible(true);
    }

    update_bar();
}

void TabWidget::resize_event(ResizeEvent& event)
{
    if (!m_active_widget)
        return;
    m_active_widget->set_relative_rect(child_rect_for_size(event.size()));
}

Rect TabWidget::child_rect_for_size(const Size& size) const
{
    Rect rect;
    switch (m_tab_position) {
    case TabPosition::Top:
        rect = { { container_padding(), bar_height() + container_padding() }, { size.width() - container_padding() * 2, size.height() - bar_height() - container_padding() * 2 } };
        break;
    case TabPosition::Bottom:
        rect = { { container_padding(), container_padding() }, { size.width() - container_padding() * 2, size.height() - bar_height() - container_padding() * 2 } };
        break;
    }
    if (rect.is_empty())
        return {};
    return rect;
}

void TabWidget::child_event(Core::ChildEvent& event)
{
    if (!event.child() || !Core::is<Widget>(*event.child()))
        return Widget::child_event(event);
    auto& child = Core::to<Widget>(*event.child());
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

Rect TabWidget::bar_rect() const
{
    switch (m_tab_position) {
    case TabPosition::Top:
        return { 0, 0, width(), bar_height() };
    case TabPosition::Bottom:
        return { 0, height() - bar_height(), width(), bar_height() };
    }
    ASSERT_NOT_REACHED();
}

Rect TabWidget::container_rect() const
{
    switch (m_tab_position) {
    case TabPosition::Top:
        return { 0, bar_height(), width(), height() - bar_height() };
    case TabPosition::Bottom:
        return { 0, 0, width(), height() - bar_height() };
    }
    ASSERT_NOT_REACHED();
}

void TabWidget::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto container_rect = this->container_rect();
    auto padding_rect = container_rect;
    for (int i = 0; i < container_padding(); ++i) {
        painter.draw_rect(padding_rect, palette().button());
        padding_rect.shrink(2, 2);
    }

    StylePainter::paint_frame(painter, container_rect, palette(), FrameShape::Container, FrameShadow::Raised, 2);

    for (int i = 0; i < m_tabs.size(); ++i) {
        if (m_tabs[i].widget == m_active_widget)
            continue;
        bool hovered = i == m_hovered_tab_index;
        auto button_rect = this->button_rect(i);
        StylePainter::paint_tab_button(painter, button_rect, palette(), false, hovered, m_tabs[i].widget->is_enabled());
        painter.draw_text(button_rect.translated(0, 1), m_tabs[i].title, TextAlignment::Center, palette().button_text());
    }

    for (int i = 0; i < m_tabs.size(); ++i) {
        if (m_tabs[i].widget != m_active_widget)
            continue;
        bool hovered = i == m_hovered_tab_index;
        auto button_rect = this->button_rect(i);
        StylePainter::paint_tab_button(painter, button_rect, palette(), true, hovered, m_tabs[i].widget->is_enabled());
        painter.draw_text(button_rect.translated(0, 1), m_tabs[i].title, TextAlignment::Center, palette().button_text());
        painter.draw_line(button_rect.bottom_left().translated(1, 1), button_rect.bottom_right().translated(-1, 1), palette().button());
        break;
    }
}

Rect TabWidget::button_rect(int index) const
{
    int x_offset = 2;
    for (int i = 0; i < index; ++i)
        x_offset += m_tabs[i].width(font());
    Rect rect { x_offset, 0, m_tabs[index].width(font()), bar_height() };
    if (m_tabs[index].widget != m_active_widget) {
        rect.move_by(0, 2);
        rect.set_height(rect.height() - 2);
    } else {
        rect.move_by(-2, 0);
        rect.set_width(rect.width() + 4);
    }
    rect.move_by(bar_rect().location());
    return rect;
}

int TabWidget::TabData::width(const Font& font) const
{
    return 16 + font.width(title);
}

void TabWidget::mousedown_event(MouseEvent& event)
{
    for (int i = 0; i < m_tabs.size(); ++i) {
        auto button_rect = this->button_rect(i);
        if (!button_rect.contains(event.position()))
            continue;
        set_active_widget(m_tabs[i].widget);
        return;
    }
}

void TabWidget::mousemove_event(MouseEvent& event)
{
    int hovered_tab = -1;
    for (int i = 0; i < m_tabs.size(); ++i) {
        auto button_rect = this->button_rect(i);
        if (!button_rect.contains(event.position()))
            continue;
        hovered_tab = i;
        if (m_tabs[i].widget == m_active_widget)
            break;
    }
    if (hovered_tab == m_hovered_tab_index)
        return;
    m_hovered_tab_index = hovered_tab;
    update_bar();
}

void TabWidget::leave_event(Core::Event&)
{
    if (m_hovered_tab_index != -1) {
        m_hovered_tab_index = -1;
        update_bar();
    }
}

void TabWidget::update_bar()
{
    auto invalidation_rect = bar_rect();
    invalidation_rect.set_height(invalidation_rect.height() + 1);
    update(invalidation_rect);
}

void TabWidget::set_tab_position(TabPosition tab_position)
{
    if (m_tab_position == tab_position)
        return;
    m_tab_position = tab_position;
    if (m_active_widget)
        m_active_widget->set_relative_rect(child_rect_for_size(size()));
    update();
}

int TabWidget::active_tab_index() const
{
    for (int i = 0; i < m_tabs.size(); i++) {
        if (m_tabs.at(i).widget == m_active_widget)
            return i;
    }
    return -1;
}
}
