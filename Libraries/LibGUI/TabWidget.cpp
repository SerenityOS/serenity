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
#include <LibGUI/Painter.h>
#include <LibGUI/TabWidget.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace GUI {

TabWidget::TabWidget()
{
}

TabWidget::~TabWidget()
{
}

void TabWidget::add_widget(const StringView& title, Widget& widget)
{
    m_tabs.append({ title, nullptr, &widget });
    add_child(widget);
}

void TabWidget::remove_widget(Widget& widget)
{
    if (active_widget() == &widget)
        activate_next_tab();
    m_tabs.remove_first_matching([&widget](auto& entry) { return &widget == entry.widget; });
    remove_child(widget);
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
        m_active_widget->set_focus(true);
        m_active_widget->set_visible(true);
        deferred_invoke([this](auto&) {
            if (on_change)
                on_change(*m_active_widget);
        });
    }

    update_bar();
}

void TabWidget::resize_event(ResizeEvent& event)
{
    if (!m_active_widget)
        return;
    m_active_widget->set_relative_rect(child_rect_for_size(event.size()));
}

Gfx::Rect TabWidget::child_rect_for_size(const Gfx::Size& size) const
{
    Gfx::Rect rect;
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

Gfx::Rect TabWidget::bar_rect() const
{
    switch (m_tab_position) {
    case TabPosition::Top:
        return { 0, 0, width(), bar_height() };
    case TabPosition::Bottom:
        return { 0, height() - bar_height(), width(), bar_height() };
    }
    ASSERT_NOT_REACHED();
}

Gfx::Rect TabWidget::container_rect() const
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

    if (container_padding() > 0)
        Gfx::StylePainter::paint_frame(painter, container_rect, palette(), Gfx::FrameShape::Container, Gfx::FrameShadow::Raised, 2);

    auto paint_tab_icon_if_needed = [&](auto& icon, auto& button_rect, auto& text_rect) {
        if (!icon)
            return;
        Gfx::Rect icon_rect { button_rect.x(), button_rect.y(), 16, 16 };
        icon_rect.move_by(4, 3);
        painter.draw_scaled_bitmap(icon_rect, *icon, icon->rect());
        text_rect.set_x(icon_rect.right() + 1 + 4);
        text_rect.intersect(button_rect);
    };

    for (size_t i = 0; i < m_tabs.size(); ++i) {
        if (m_tabs[i].widget == m_active_widget)
            continue;
        bool hovered = static_cast<int>(i) == m_hovered_tab_index;
        auto button_rect = this->button_rect(i);
        Gfx::StylePainter::paint_tab_button(painter, button_rect, palette(), false, hovered, m_tabs[i].widget->is_enabled());
        auto text_rect = button_rect.translated(0, 1);
        paint_tab_icon_if_needed(m_tabs[i].icon, button_rect, text_rect);
        painter.draw_text(text_rect, m_tabs[i].title, m_text_alignment, palette().button_text(), Gfx::TextElision::Right);
    }

    for (size_t i = 0; i < m_tabs.size(); ++i) {
        if (m_tabs[i].widget != m_active_widget)
            continue;
        bool hovered = static_cast<int>(i) == m_hovered_tab_index;
        auto button_rect = this->button_rect(i);
        Gfx::StylePainter::paint_tab_button(painter, button_rect, palette(), true, hovered, m_tabs[i].widget->is_enabled());
        auto text_rect = button_rect.translated(0, 1);
        paint_tab_icon_if_needed(m_tabs[i].icon, button_rect, text_rect);
        painter.draw_text(text_rect, m_tabs[i].title, m_text_alignment, palette().button_text(), Gfx::TextElision::Right);
        painter.draw_line(button_rect.bottom_left().translated(1, 1), button_rect.bottom_right().translated(-1, 1), palette().button());
        break;
    }
}

int TabWidget::uniform_tab_width() const
{
    return 160;
}

Gfx::Rect TabWidget::button_rect(int index) const
{
    int x_offset = 2;
    for (int i = 0; i < index; ++i) {
        auto tab_width = m_uniform_tabs ? uniform_tab_width() : m_tabs[i].width(font());
        x_offset += tab_width;
    }
    Gfx::Rect rect { x_offset, 0, m_uniform_tabs ? uniform_tab_width() : m_tabs[index].width(font()), bar_height() };
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

int TabWidget::TabData::width(const Gfx::Font& font) const
{
    return 16 + font.width(title) + (icon ? (16 + 4) : 0);
}

void TabWidget::mousedown_event(MouseEvent& event)
{
    for (size_t i = 0; i < m_tabs.size(); ++i) {
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
    for (size_t i = 0; i < m_tabs.size(); ++i) {
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
    for (size_t i = 0; i < m_tabs.size(); i++) {
        if (m_tabs.at(i).widget == m_active_widget)
            return i;
    }
    return -1;
}

void TabWidget::set_tab_title(Widget& tab, const StringView& title)
{
    for (auto& t : m_tabs) {
        if (t.widget == &tab) {
            if (t.title != title) {
                t.title = title;
                update();
            }
            return;
        }
    }
}

void TabWidget::set_tab_icon(Widget& tab, const Gfx::Bitmap* icon)
{
    for (auto& t : m_tabs) {
        if (t.widget == &tab) {
            t.icon = icon;
            update();
            return;
        }
    }
}

void TabWidget::activate_next_tab()
{
    if (m_tabs.size() <= 1)
        return;
    int index = active_tab_index();
    ++index;
    if (index >= (int)m_tabs.size())
        index = 0;
    set_active_widget(m_tabs.at(index).widget);
}

void TabWidget::activate_previous_tab()
{
    if (m_tabs.size() <= 1)
        return;
    int index = active_tab_index();
    --index;
    if (index < 0)
        index = m_tabs.size() - 1;
    set_active_widget(m_tabs.at(index).widget);
}

void TabWidget::keydown_event(KeyEvent & event)
{
    if (event.ctrl() && event.key() == Key_Tab) {
        if (event.shift())
            activate_previous_tab();
        else
            activate_next_tab();
        event.accept();
        return;
    }
    Widget::keydown_event(event);
}

}
