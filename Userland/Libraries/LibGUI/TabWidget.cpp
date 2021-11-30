/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, TabWidget)

namespace GUI {

TabWidget::TabWidget()
{
    set_focus_policy(FocusPolicy::NoFocus);

    REGISTER_MARGINS_PROPERTY("container_margins", container_margins, set_container_margins);
    REGISTER_BOOL_PROPERTY("uniform_tabs", uniform_tabs, set_uniform_tabs);

    register_property(
        "text_alignment",
        [this] { return Gfx::to_string(text_alignment()); },
        [this](auto& value) {
            auto alignment = Gfx::text_alignment_from_string(value.to_string());
            if (alignment.has_value()) {
                set_text_alignment(alignment.value());
                return true;
            }
            return false;
        });
}

TabWidget::~TabWidget()
{
}

ErrorOr<void> TabWidget::try_add_widget(String title, Widget& widget)
{
    m_tabs.append({ move(title), nullptr, &widget });
    add_child(widget);
    update_focus_policy();
    if (on_tab_count_change)
        on_tab_count_change(m_tabs.size());
    return {};
}

void TabWidget::add_widget(String title, Widget& widget)
{
    MUST(try_add_widget(move(title), widget));
}

void TabWidget::remove_widget(Widget& widget)
{
    VERIFY(widget.parent() == this);
    auto tab_index = m_tabs.find_if([&widget](auto& entry) { return &widget == entry.widget; }).index();

    auto is_active = active_widget() == &widget;
    m_tabs.remove(tab_index);
    remove_child(widget);

    if (is_active && m_tabs.size() > 0) {
        auto next_tab_index = tab_index >= m_tabs.size() ? m_tabs.size() - 1 : tab_index;
        set_tab_index(next_tab_index);
    }

    update_focus_policy();
    if (on_tab_count_change)
        on_tab_count_change(m_tabs.size());
}

void TabWidget::remove_all_tabs_except(Widget& widget)
{
    VERIFY(widget.parent() == this);
    set_active_widget(&widget);
    m_tabs.remove_all_matching([this, &widget](auto& entry) {
        bool is_other = &widget != entry.widget;
        if (is_other)
            remove_child(*entry.widget);
        return is_other;
    });
    VERIFY(m_tabs.size() == 1);
    update_focus_policy();
    if (on_tab_count_change)
        on_tab_count_change(1);
}

void TabWidget::update_focus_policy()
{
    FocusPolicy policy;
    if (is_bar_visible() && !m_tabs.is_empty())
        policy = FocusPolicy::TabFocus;
    else
        policy = FocusPolicy::NoFocus;
    set_focus_policy(policy);
}

void TabWidget::set_active_widget(Widget* widget)
{
    if (widget == m_active_widget)
        return;

    bool active_widget_had_focus = m_active_widget && m_active_widget->has_focus_within();

    if (m_active_widget)
        m_active_widget->set_visible(false);
    m_active_widget = widget;
    if (m_active_widget) {
        m_active_widget->set_relative_rect(child_rect_for_size(size()));
        if (active_widget_had_focus)
            m_active_widget->set_focus(true);
        m_active_widget->set_visible(true);
        deferred_invoke([this] {
            if (on_change)
                on_change(*m_active_widget);
        });
    }

    update_bar();
}

void TabWidget::set_tab_index(int index)
{
    if (m_tabs.at(index).widget == m_active_widget)
        return;
    set_active_widget(m_tabs.at(index).widget);

    update_bar();
}

void TabWidget::resize_event(ResizeEvent& event)
{
    if (!m_active_widget)
        return;
    m_active_widget->set_relative_rect(child_rect_for_size(event.size()));
}

Gfx::IntRect TabWidget::child_rect_for_size(const Gfx::IntSize& size) const
{
    Gfx::IntRect rect;
    switch (m_tab_position) {
    case TabPosition::Top:
        rect = { { m_container_margins.left(), bar_height() + m_container_margins.top() }, { size.width() - m_container_margins.left() - m_container_margins.right(), size.height() - bar_height() - m_container_margins.top() - m_container_margins.bottom() } };
        break;
    case TabPosition::Bottom:
        rect = { { m_container_margins.left(), m_container_margins.top() }, { size.width() - m_container_margins.left() - m_container_margins.right(), size.height() - bar_height() - m_container_margins.top() - m_container_margins.bottom() } };
        break;
    }
    if (rect.is_empty())
        return {};
    return rect;
}

void TabWidget::child_event(Core::ChildEvent& event)
{
    if (!event.child() || !is<Widget>(*event.child()))
        return Widget::child_event(event);
    auto& child = verify_cast<Widget>(*event.child());
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

Gfx::IntRect TabWidget::bar_rect() const
{
    switch (m_tab_position) {
    case TabPosition::Top:
        return { 0, 0, width(), bar_height() };
    case TabPosition::Bottom:
        return { 0, height() - bar_height(), width(), bar_height() };
    }
    VERIFY_NOT_REACHED();
}

Gfx::IntRect TabWidget::container_rect() const
{
    switch (m_tab_position) {
    case TabPosition::Top:
        return { 0, bar_height(), width(), height() - bar_height() };
    case TabPosition::Bottom:
        return { 0, 0, width(), height() - bar_height() };
    }
    VERIFY_NOT_REACHED();
}

void TabWidget::paint_event(PaintEvent& event)
{
    if (!m_bar_visible)
        return;

    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), palette().button());

    if (!m_container_margins.is_null()) {
        Gfx::StylePainter::paint_frame(painter, container_rect(), palette(), Gfx::FrameShape::Container, Gfx::FrameShadow::Raised, 2);
    }

    auto paint_tab_icon_if_needed = [&](auto& icon, auto& button_rect, auto& text_rect) {
        if (!icon)
            return;
        Gfx::IntRect icon_rect { button_rect.x(), button_rect.y(), 16, 16 };
        icon_rect.translate_by(4, 3);
        painter.draw_scaled_bitmap(icon_rect, *icon, icon->rect());
        text_rect.set_x(icon_rect.right() + 1 + 4);
        text_rect.intersect(button_rect);

        // We want to be in perfect alignment with the icon rect at all times.
        auto icon_rect_difference = icon_rect.top() - text_rect.top();
        text_rect.set_top(text_rect.top() + icon_rect_difference);
        text_rect.set_height(text_rect.height() - icon_rect_difference);

        // ...unless our leftover height after text drawing is uneven, in which
        // case we want to bias towards the bottom when the tab position is at
        // the top.
        if ((text_rect.height() - font().glyph_height()) % 2 != 0 && m_tab_position == TabPosition::Top) {
            text_rect.set_top(text_rect.top() + 1);
        }
    };

    for (size_t i = 0; i < m_tabs.size(); ++i) {
        if (m_tabs[i].widget == m_active_widget)
            continue;
        bool hovered = i == m_hovered_tab_index;
        auto button_rect = this->button_rect(i);
        Gfx::StylePainter::paint_tab_button(painter, button_rect, palette(), false, hovered, m_tabs[i].widget->is_enabled(), m_tab_position == TabPosition::Top, window()->is_active());

        // First we get rid of the 1px sheen on the left, then the 2px shadow
        // on the right. Finally we shrink 3px from each side.
        auto tab_button_content_rect = button_rect.shrunken(2, 0);
        tab_button_content_rect.set_width(tab_button_content_rect.width() - 1);
        tab_button_content_rect.shrink(6, 0);
        if (m_tab_position == TabPosition::Top) {
            tab_button_content_rect.set_top(tab_button_content_rect.top() + 1);
            tab_button_content_rect.set_height(tab_button_content_rect.height() - 1);
        }

        paint_tab_icon_if_needed(m_tabs[i].icon, button_rect, tab_button_content_rect);
        tab_button_content_rect.set_width(tab_button_content_rect.width() - (m_close_button_enabled ? 16 : 0));

        painter.draw_text(tab_button_content_rect, m_tabs[i].title, m_text_alignment, palette().button_text(), Gfx::TextElision::Right);
    }

    if (m_close_button_enabled) {
        for (size_t i = 0; i < m_tabs.size(); ++i) {
            if (m_tabs[i].widget == m_active_widget)
                continue;

            bool hovered_close_button = i == m_hovered_close_button_index;
            bool pressed_close_button = i == m_pressed_close_button_index;
            auto close_button_rect = this->close_button_rect(i);

            if (hovered_close_button)
                Gfx::StylePainter::paint_frame(painter, close_button_rect, palette(), Gfx::FrameShape::Box, pressed_close_button ? Gfx::FrameShadow::Sunken : Gfx::FrameShadow::Raised, 1);

            Gfx::IntRect icon_rect { close_button_rect.x() + 3, close_button_rect.y() + 3, 6, 6 };
            painter.draw_line(icon_rect.top_left(), icon_rect.bottom_right(), palette().button_text());
            painter.draw_line(icon_rect.top_right(), icon_rect.bottom_left(), palette().button_text());
        }
    }

    for (size_t i = 0; i < m_tabs.size(); ++i) {
        if (m_tabs[i].widget != m_active_widget)
            continue;

        bool hovered = i == m_hovered_tab_index;
        auto button_rect = this->button_rect(i);

        if (m_dragging_active_tab)
            button_rect.set_x(m_mouse_x - m_grab_offset);

        Gfx::StylePainter::paint_tab_button(painter, button_rect, palette(), true, hovered, m_tabs[i].widget->is_enabled(), m_tab_position == TabPosition::Top, window()->is_active());

        // First we get rid of the 1px sheen on the left, then the 2px shadow
        // on the right. Finally we shrink 3px from each side.
        auto tab_button_content_rect = button_rect.shrunken(2, 0);
        tab_button_content_rect.set_width(tab_button_content_rect.width() - 1);
        tab_button_content_rect.shrink(6, 0);
        if (m_tab_position == TabPosition::Top) {
            tab_button_content_rect.set_top(tab_button_content_rect.top() + 1);
            tab_button_content_rect.set_height(tab_button_content_rect.height() - 1);
        }

        paint_tab_icon_if_needed(m_tabs[i].icon, button_rect, tab_button_content_rect);
        tab_button_content_rect.set_width(tab_button_content_rect.width() - (m_close_button_enabled ? 16 : 0));

        painter.draw_text(tab_button_content_rect, m_tabs[i].title, m_text_alignment, palette().button_text(), Gfx::TextElision::Right);

        if (is_focused()) {
            Gfx::IntRect focus_rect { 0, 0, min(tab_button_content_rect.width(), font().width(m_tabs[i].title)), font().glyph_height() };
            focus_rect.align_within(tab_button_content_rect, m_text_alignment);
            focus_rect.inflate(6, 4);

            painter.draw_focus_rect(focus_rect, palette().focus_outline());
        }

        if (m_tab_position == TabPosition::Top) {
            painter.draw_line(button_rect.bottom_left().translated(1, 1), button_rect.bottom_right().translated(-1, 1), palette().button());
        } else if (m_tab_position == TabPosition::Bottom) {
            painter.set_pixel(button_rect.top_left().translated(0, -1), palette().threed_highlight());
            painter.set_pixel(button_rect.top_right().translated(-1, -1), palette().threed_shadow1());
            painter.draw_line(button_rect.top_left().translated(1, -1), button_rect.top_right().translated(-2, -1), palette().button());
            painter.draw_line(button_rect.top_left().translated(1, -2), button_rect.top_right().translated(-2, -2), palette().button());
        }
        break;
    }

    if (!m_close_button_enabled)
        return;

    for (size_t i = 0; i < m_tabs.size(); ++i) {
        if (m_tabs[i].widget != m_active_widget)
            continue;

        bool hovered_close_button = i == m_hovered_close_button_index;
        bool pressed_close_button = i == m_pressed_close_button_index;
        auto close_button_rect = this->close_button_rect(i);

        if (m_dragging_active_tab)
            close_button_rect.set_x((m_mouse_x - m_grab_offset) + (close_button_rect.x() - button_rect(i).x()));

        if (hovered_close_button)
            Gfx::StylePainter::paint_frame(painter, close_button_rect, palette(), Gfx::FrameShape::Box, pressed_close_button ? Gfx::FrameShadow::Sunken : Gfx::FrameShadow::Raised, 1);

        Gfx::IntRect icon_rect { close_button_rect.x() + 3, close_button_rect.y() + 3, 6, 6 };
        painter.draw_line(icon_rect.top_left(), icon_rect.bottom_right(), palette().button_text());
        painter.draw_line(icon_rect.top_right(), icon_rect.bottom_left(), palette().button_text());
    }
}

int TabWidget::uniform_tab_width() const
{
    int minimum_tab_width = 24;
    int maximum_tab_width = 160;
    int total_tab_width = m_tabs.size() * maximum_tab_width;
    int tab_width = maximum_tab_width;
    int available_width = width() - bar_margin() * 2;
    if (total_tab_width > available_width)
        tab_width = available_width / m_tabs.size();
    return max(tab_width, minimum_tab_width);
}

void TabWidget::set_bar_visible(bool bar_visible)
{
    m_bar_visible = bar_visible;
    if (m_active_widget)
        m_active_widget->set_relative_rect(child_rect_for_size(size()));
    update_bar();
}

Gfx::IntRect TabWidget::button_rect(size_t index) const
{
    int x_offset = bar_margin();
    int close_button_offset = m_close_button_enabled ? 16 : 0;

    for (size_t i = 0; i < index; ++i) {
        auto tab_width = m_uniform_tabs ? uniform_tab_width() : m_tabs[i].width(font()) + close_button_offset;
        x_offset += tab_width;
    }
    Gfx::IntRect rect { x_offset, 0, m_uniform_tabs ? uniform_tab_width() : m_tabs[index].width(font()) + close_button_offset, bar_height() };
    if (m_tabs[index].widget != m_active_widget) {
        rect.translate_by(0, m_tab_position == TabPosition::Top ? 2 : 0);
        rect.set_height(rect.height() - 2);
    } else {
        rect.translate_by(-2, 0);
        rect.set_width(rect.width() + 4);
    }
    rect.translate_by(bar_rect().location());
    return rect;
}

Gfx::IntRect TabWidget::close_button_rect(size_t index) const
{
    auto rect = button_rect(index);
    Gfx::IntRect close_button_rect { 0, 0, 12, 12 };

    if (m_tabs[index].widget == m_active_widget)
        close_button_rect.translate_by(rect.right() - 16, rect.top() + (m_tab_position == TabPosition::Top ? 5 : 4));
    else
        close_button_rect.translate_by(rect.right() - 15, rect.top() + (m_tab_position == TabPosition::Top ? 4 : 3));

    return close_button_rect;
}

int TabWidget::TabData::width(const Gfx::Font& font) const
{
    auto width = 16 + font.width(title) + (icon ? (16 + 4) : 0);
    // NOTE: This needs to always be an odd number, because the button rect
    //       includes 3px of light and shadow on the left and right edges. If
    //       the button rect width is not an odd number, the area left for the
    //       text and the focus rect has an odd number of pixels, and this
    //       causes the text (and subsequently the focus rect) to not be aligned
    //       to the center perfectly.
    if (width % 2 == 0) {
        width++;
    }

    return width;
}

void TabWidget::mousedown_event(MouseEvent& event)
{
    for (size_t i = 0; i < m_tabs.size(); ++i) {
        auto button_rect = this->button_rect(i);
        auto close_button_rect = this->close_button_rect(i);

        if (!button_rect.contains(event.position()))
            continue;

        if (event.button() == MouseButton::Primary) {
            if (m_close_button_enabled && close_button_rect.contains(event.position())) {
                m_pressed_close_button_index = i;
                update_bar();
                return;
            }
            set_active_widget(m_tabs[i].widget);
            drag_tab(i);
        } else if (event.button() == MouseButton::Middle) {
            auto* widget = m_tabs[i].widget;
            deferred_invoke([this, widget] {
                if (on_middle_click && widget)
                    on_middle_click(*widget);
            });
        }
        return;
    }
}

void TabWidget::mouseup_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;

    if (m_dragging_active_tab) {
        m_dragging_active_tab = false;
        update_bar();
    }

    if (!m_close_button_enabled || !m_pressed_close_button_index.has_value())
        return;

    auto close_button_rect = this->close_button_rect(m_pressed_close_button_index.value());
    update_bar();

    if (close_button_rect.contains(event.position())) {
        auto* widget = m_tabs[m_pressed_close_button_index.value()].widget;
        deferred_invoke([this, widget] {
            if (on_tab_close_click && widget)
                on_tab_close_click(*widget);
        });
    }
    m_pressed_close_button_index = {};
}

void TabWidget::mousemove_event(MouseEvent& event)
{
    Optional<size_t> hovered_tab = {};
    Optional<size_t> hovered_close_button = {};

    m_mouse_x = event.position().x();
    if (m_dragging_active_tab) {
        recalculate_tab_order();
        update_bar();
        return;
    }

    for (size_t i = 0; i < m_tabs.size(); ++i) {
        auto button_rect = this->button_rect(i);
        auto close_button_rect = this->close_button_rect(i);

        if (close_button_rect.contains(event.position()))
            hovered_close_button = i;

        if (!button_rect.contains(event.position()))
            continue;
        hovered_tab = i;
        if (m_tabs[i].widget == m_active_widget)
            break;
    }
    if (!hovered_tab.has_value() && !hovered_close_button.has_value())
        return;
    m_hovered_tab_index = hovered_tab;
    m_hovered_close_button_index = hovered_close_button;
    update_bar();
}

void TabWidget::leave_event(Core::Event&)
{
    if (m_hovered_tab_index.has_value() || m_hovered_close_button_index.has_value()) {
        m_hovered_tab_index = {};
        m_hovered_close_button_index = {};
        update_bar();
    }
}

void TabWidget::update_bar()
{
    if (m_tabs.is_empty())
        return;
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

Optional<size_t> TabWidget::active_tab_index() const
{
    for (size_t i = 0; i < m_tabs.size(); i++) {
        if (m_tabs.at(i).widget == m_active_widget)
            return i;
    }
    return {};
}

void TabWidget::set_tab_title(Widget& tab, StringView title)
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
    auto index = active_tab_index();
    if (!index.has_value())
        return;
    auto next_index = index.value() + 1;
    if (next_index >= m_tabs.size())
        next_index = 0;
    set_active_widget(m_tabs.at(next_index).widget);
}

void TabWidget::activate_previous_tab()
{
    if (m_tabs.size() <= 1)
        return;
    auto index = active_tab_index();
    if (!index.has_value())
        return;
    size_t previous_index = 0;
    if (index.value() == 0)
        previous_index = m_tabs.size() - 1;
    else
        previous_index = index.value() - 1;
    set_active_widget(m_tabs.at(previous_index).widget);
}

void TabWidget::keydown_event(KeyEvent& event)
{
    if (event.ctrl() && event.key() == Key_Tab) {
        if (event.shift())
            activate_previous_tab();
        else
            activate_next_tab();
        event.accept();
        return;
    }
    if (is_focused()) {
        if (!event.modifiers() && event.key() == Key_Left) {
            activate_previous_tab();
            event.accept();
            return;
        }
        if (!event.modifiers() && event.key() == Key_Right) {
            activate_next_tab();
            event.accept();
            return;
        }
    }
    Widget::keydown_event(event);
}

void TabWidget::context_menu_event(ContextMenuEvent& context_menu_event)
{
    for (size_t i = 0; i < m_tabs.size(); ++i) {
        auto button_rect = this->button_rect(i);
        if (!button_rect.contains(context_menu_event.position()))
            continue;
        auto* widget = m_tabs[i].widget;
        deferred_invoke([this, widget, context_menu_event] {
            if (on_context_menu_request && widget)
                on_context_menu_request(*widget, context_menu_event);
        });
        return;
    }
}

void TabWidget::doubleclick_event(MouseEvent&)
{
    for (auto& tab : m_tabs) {
        if (auto* widget = tab.widget) {
            deferred_invoke([this, widget] {
                if (on_double_click)
                    on_double_click(*widget);
            });
        }
    }
}

void TabWidget::set_container_margins(GUI::Margins const& margins)
{
    m_container_margins = margins;
    update();
}

void TabWidget::drag_tab(size_t index)
{
    m_dragging_active_tab = m_reorder_allowed;
    m_grab_offset = m_mouse_x - button_rect(index).x();
    m_hovered_tab_index = {};
    m_hovered_close_button_index = {};
}

void TabWidget::recalculate_tab_order()
{
    if (!m_dragging_active_tab)
        return;

    size_t active;
    for (active = 0; active < m_tabs.size(); ++active) {
        if (m_tabs[active].widget == m_active_widget)
            break;
    }

    size_t target;
    for (target = 0; target < active; ++target) {
        auto button_rect = this->button_rect(target);
        if ((m_mouse_x - m_grab_offset) < button_rect.x()) {
            break;
        }
    }

    if (target == active) {
        for (target = m_tabs.size() - 1; target > active; --target) {
            auto button_rect = this->button_rect(target);
            if ((m_mouse_x - m_grab_offset) > button_rect.x()) {
                break;
            }
        }
    }

    if (active == target)
        return;

    auto tab = m_tabs.take(active);
    m_tabs.insert(target, tab);
    update_bar();
}
}
