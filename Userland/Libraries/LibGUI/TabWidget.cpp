/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2022, Cameron Youell <cameronyouell@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericShorthands.h>
#include <AK/JsonValue.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, TabWidget)

namespace GUI {

TabWidget::TabWidget()
{
    set_focus_policy(FocusPolicy::NoFocus);

    REGISTER_MARGINS_PROPERTY("container_margins", container_margins, set_container_margins);
    REGISTER_BOOL_PROPERTY("show_close_buttons", close_button_enabled, set_close_button_enabled);
    REGISTER_BOOL_PROPERTY("show_tab_bar", is_bar_visible, set_bar_visible);
    REGISTER_BOOL_PROPERTY("reorder_allowed", reorder_allowed, set_reorder_allowed);
    REGISTER_BOOL_PROPERTY("uniform_tabs", uniform_tabs, set_uniform_tabs);
    REGISTER_ENUM_PROPERTY("tab_position", this->tab_position, set_tab_position, TabPosition,
        { TabPosition::Top, "Top" },
        { TabPosition::Bottom, "Bottom" },
        { TabPosition::Left, "Left" },
        { TabPosition::Right, "Right" }, );
    REGISTER_TEXT_ALIGNMENT_PROPERTY("text_alignment", text_alignment, set_text_alignment);
}

ErrorOr<void> TabWidget::try_add_widget(Widget& widget)
{
    TRY(m_tabs.try_append({ widget.title(), nullptr, nullptr, &widget, false }));
    TRY(try_add_child(widget));
    update_focus_policy();
    if (on_tab_count_change)
        on_tab_count_change(m_tabs.size());
    layout_relevant_change_occurred();
    return {};
}

void TabWidget::add_widget(Widget& widget)
{
    MUST(try_add_widget(widget));
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

    layout_relevant_change_occurred();
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

    layout_relevant_change_occurred();
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

    layout_relevant_change_occurred();

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

Gfx::IntRect TabWidget::child_rect_for_size(Gfx::IntSize size) const
{
    Gfx::IntRect rect;
    switch (m_tab_position) {
    case TabPosition::Top:
        rect = { { m_container_margins.left(), bar_height() + m_container_margins.top() }, { size.width() - m_container_margins.left() - m_container_margins.right(), size.height() - bar_height() - m_container_margins.top() - m_container_margins.bottom() } };
        break;
    case TabPosition::Bottom:
        rect = { { m_container_margins.left(), m_container_margins.top() }, { size.width() - m_container_margins.left() - m_container_margins.right(), size.height() - bar_height() - m_container_margins.top() - m_container_margins.bottom() } };
        break;
    case TabPosition::Left:
        rect = { { get_max_tab_width() + m_container_margins.left(), m_container_margins.top() }, { size.width() - get_max_tab_width() - m_container_margins.left() - m_container_margins.right(), size.height() - m_container_margins.top() - m_container_margins.bottom() } };
        break;
    case TabPosition::Right:
        rect = { { m_container_margins.left(), m_container_margins.top() }, { size.width() - get_max_tab_width() - m_container_margins.left() - m_container_margins.right(), size.height() - m_container_margins.top() - m_container_margins.bottom() } };
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
    case TabPosition::Left:
        return { 0, 0, get_max_tab_width(), height() };
    case TabPosition::Right:
        return { width() - get_max_tab_width(), 0, get_max_tab_width(), height() };
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
    case TabPosition::Left:
        return { get_max_tab_width(), 0, width() - get_max_tab_width(), height() };
    case TabPosition::Right:
        return { 0, 0, width() - get_max_tab_width(), height() };
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
        Gfx::StylePainter::paint_frame(painter, container_rect(), palette(), Gfx::FrameStyle::RaisedContainer);
    }

    auto make_icon_rect = [](auto const& button_rect) {
        Gfx::IntRect icon_rect { button_rect.x(), button_rect.y(), 16, 16 };
        icon_rect.translate_by(4, (button_rect.height() / 2) - (icon_rect.height() / 2));

        return icon_rect;
    };

    auto paint_tab_icon_if_needed = [&](auto& icon, auto& button_rect, auto& icon_rect, auto& text_rect) {
        if (!icon)
            return;

        painter.draw_scaled_bitmap(icon_rect, *icon, icon->rect());

        text_rect.set_x(icon_rect.right() + 4);
        text_rect.intersect(button_rect);

        icon_rect.set_x(text_rect.x());
    };

    bool accented = Desktop::the().system_effects().tab_accents();

    for (size_t i = 0; i < m_tabs.size(); ++i) {
        if (m_tabs[i].widget == m_active_widget)
            continue;
        bool hovered = i == m_hovered_tab_index;
        auto button_rect = this->button_rect(i);
        Gfx::StylePainter::paint_tab_button(painter, button_rect, palette(), false, hovered, m_tabs[i].widget->is_enabled(), m_tab_position, window()->is_active(), accented);

        auto tab_button_content_rect = button_rect.shrunken(8, 0);
        auto icon_rect = make_icon_rect(button_rect);

        paint_tab_icon_if_needed(m_tabs[i].action_icon, button_rect, icon_rect, tab_button_content_rect);
        paint_tab_icon_if_needed(m_tabs[i].icon, button_rect, icon_rect, tab_button_content_rect);
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
                Gfx::StylePainter::paint_frame(painter, close_button_rect, palette(), pressed_close_button ? Gfx::FrameStyle::SunkenPanel : Gfx::FrameStyle::RaisedPanel);

            Gfx::IntRect icon_rect { close_button_rect.x() + 3, close_button_rect.y() + 3, 6, 6 };
            if (!m_tabs[i].modified) {
                painter.draw_line(icon_rect.top_left(), icon_rect.bottom_right().translated(-1), palette().button_text());
                painter.draw_line(icon_rect.top_right().moved_left(1), icon_rect.bottom_left().moved_up(1), palette().button_text());
            } else {
                painter.draw_line(icon_rect.top_left().moved_right(1), icon_rect.bottom_right().translated(-2), palette().button_text());
                painter.draw_line(icon_rect.top_right().moved_left(2), icon_rect.bottom_left().translated(1, -2), palette().button_text());
                painter.draw_line(icon_rect.bottom_left(), icon_rect.bottom_right().moved_left(1), palette().button_text(), 1, Gfx::LineStyle::Dotted);
            }
        }
    }

    for (size_t i = 0; i < m_tabs.size(); ++i) {
        if (m_tabs[i].widget != m_active_widget)
            continue;

        bool hovered = i == m_hovered_tab_index;
        auto button_rect = this->button_rect(i);

        if (m_dragging_active_tab) {
            if (this->has_vertical_tabs())
                button_rect.set_y(m_mouse_pos - m_grab_offset);
            else
                button_rect.set_x(m_mouse_pos - m_grab_offset);
        }

        auto tab_button_content_rect = button_rect.shrunken(8, 0);
        auto icon_rect = make_icon_rect(button_rect);

        Gfx::StylePainter::paint_tab_button(painter, button_rect, palette(), true, hovered, m_tabs[i].widget->is_enabled(), m_tab_position, window()->is_active(), accented);

        paint_tab_icon_if_needed(m_tabs[i].action_icon, button_rect, icon_rect, tab_button_content_rect);
        paint_tab_icon_if_needed(m_tabs[i].icon, button_rect, icon_rect, tab_button_content_rect);
        tab_button_content_rect.set_width(tab_button_content_rect.width() - (m_close_button_enabled ? 16 : 0));

        painter.draw_text(tab_button_content_rect, m_tabs[i].title, m_text_alignment, palette().button_text(), Gfx::TextElision::Right);

        if (is_focused()) {
            Gfx::IntRect focus_rect { 0, 0, min(tab_button_content_rect.width(), font().width(m_tabs[i].title)), font().pixel_size_rounded_up() };
            focus_rect.align_within(tab_button_content_rect, m_text_alignment);
            focus_rect.inflate(6, 4);

            painter.draw_focus_rect(focus_rect, palette().focus_outline());
        }

        if (m_tab_position == TabPosition::Top) {
            painter.draw_line(button_rect.bottom_left().moved_right(1), button_rect.bottom_right().translated(-2, 0), palette().button());
        } else if (m_tab_position == TabPosition::Bottom) {
            painter.set_pixel(button_rect.top_left().translated(0, -1), palette().threed_highlight());
            painter.set_pixel(button_rect.top_right().translated(-2, -1), palette().threed_shadow1());
            painter.draw_line(button_rect.top_left().translated(1, -1), button_rect.top_right().translated(-3, -1), palette().button());
            painter.draw_line(button_rect.top_left().translated(1, -2), button_rect.top_right().translated(-3, -2), palette().button());
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

        if (m_dragging_active_tab) {
            if (this->has_vertical_tabs())
                close_button_rect.set_y((m_mouse_pos - m_grab_offset) + (close_button_rect.y() - button_rect(i).y()));
            else
                close_button_rect.set_x((m_mouse_pos - m_grab_offset) + (close_button_rect.x() - button_rect(i).x()));
        }

        if (hovered_close_button)
            Gfx::StylePainter::paint_frame(painter, close_button_rect, palette(), pressed_close_button ? Gfx::FrameStyle::SunkenPanel : Gfx::FrameStyle::RaisedPanel);

        Gfx::IntRect icon_rect { close_button_rect.x() + 3, close_button_rect.y() + 3, 6, 6 };
        if (!m_tabs[i].modified) {
            painter.draw_line(icon_rect.top_left(), icon_rect.bottom_right().translated(-1), palette().button_text());
            painter.draw_line(icon_rect.top_right().moved_left(1), icon_rect.bottom_left().moved_up(1), palette().button_text());
        } else {
            painter.draw_line(icon_rect.top_left().moved_right(1), icon_rect.bottom_right().translated(-2, -2), palette().button_text());
            painter.draw_line(icon_rect.top_right().moved_left(2), icon_rect.bottom_left().translated(1, -2), palette().button_text());
            painter.draw_line(icon_rect.bottom_left(), icon_rect.bottom_right().moved_left(1), palette().button_text(), 1, Gfx::LineStyle::Dotted);
        }
    }
}

int TabWidget::uniform_tab_width() const
{
    int total_tab_width = m_tabs.size() * get_max_tab_width();
    int tab_width = get_max_tab_width();

    if (this->has_vertical_tabs())
        return tab_width;

    int available_width = width() - bar_margin() * 2;
    if (total_tab_width > available_width)
        tab_width = available_width / m_tabs.size();
    return max(tab_width, m_min_tab_width);
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
    if (this->has_vertical_tabs())
        return vertical_button_rect(index);
    return horizontal_button_rect(index);
}

Gfx::IntRect TabWidget::vertical_button_rect(size_t index) const
{
    int offset = bar_margin() + (bar_height() * index);
    Gfx::IntRect rect { 0, offset, get_max_tab_width() - 1, bar_height() };

    if (m_tabs[index].widget != m_active_widget) {
        rect.translate_by(m_tab_position == TabPosition::Left ? 2 : 0, 0);
        rect.set_width(rect.width() - 2);
    } else {
        rect.translate_by(0, -2);
        rect.set_height(rect.height() + 4);
    }

    rect.translate_by(bar_rect().location());
    return rect;
}

Gfx::IntRect TabWidget::horizontal_button_rect(size_t index) const
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

    close_button_rect.translate_by(rect.right() - 1, rect.top());
    close_button_rect.translate_by(-(close_button_rect.width() + 4), (rect.height() / 2) - (close_button_rect.height() / 2));

    return close_button_rect;
}

int TabWidget::TabData::width(Gfx::Font const& font) const
{
    auto width = 16 + font.width_rounded_up(title) + (icon ? (16 + 4) : 0);
    // NOTE: This needs to always be an odd number, because the button rect
    //       includes 3px of light and shadow on the left and right edges. If
    //       the button rect width is not an odd number, the area left for the
    //       text and the focus rect has an odd number of pixels, and this
    //       causes the text (and subsequently the focus rect) to not be aligned
    //       to the center perfectly.
    if (width % 2 == 0)
        width++;

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

    m_mouse_pos = this->has_vertical_tabs() ? event.position().y() : event.position().x();
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
    if (this->has_vertical_tabs())
        m_uniform_tabs = true;
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

void TabWidget::set_tab_title(Widget& tab, String title)
{
    for (auto& t : m_tabs) {
        if (t.widget == &tab) {
            if (t.title != title) {
                t.title = move(title);
                update();
            }
            return;
        }
    }
}

void TabWidget::set_tab_icon(Widget& tab, Gfx::Bitmap const* icon)
{
    for (auto& t : m_tabs) {
        if (t.widget == &tab) {
            t.icon = icon;
            update();
            return;
        }
    }
}

// FIXME: Also accept an action to be triggered when the action icon is clicked. If the action is non-null, then also
//        paint the icon as a button (with hover/click effects).
void TabWidget::set_tab_action_icon(Widget& tab, Gfx::Bitmap const* action_icon)
{
    for (auto& t : m_tabs) {
        if (t.widget == &tab) {
            t.action_icon = action_icon;
            update();
            return;
        }
    }
}

bool TabWidget::is_tab_modified(Widget& tab_input)
{
    auto it = m_tabs.find_if([&](auto t) { return t.widget == &tab_input; });
    if (it.is_end())
        return false;
    auto& tab = *it;
    return tab.modified;
}

void TabWidget::set_tab_modified(Widget& tab_input, bool modified)
{
    auto it = m_tabs.find_if([&](auto t) { return t.widget == &tab_input; });
    if (it.is_end())
        return;
    auto& tab = *it;
    if (tab.modified != modified) {
        tab.modified = modified;
        update();
    }
}

bool TabWidget::is_any_tab_modified()
{
    return any_of(m_tabs, [](auto& t) { return t.modified; });
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

void TabWidget::activate_last_tab()
{
    size_t number_of_tabs = m_tabs.size();
    if (number_of_tabs == 0)
        return;
    set_active_widget(m_tabs.at(number_of_tabs - 1).widget);
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

void TabWidget::doubleclick_event(MouseEvent& mouse_event)
{
    for (size_t i = 0; i < m_tabs.size(); ++i) {
        auto button_rect = this->button_rect(i);
        if (!button_rect.contains(mouse_event.position()))
            continue;
        if (auto* widget = m_tabs[i].widget) {
            deferred_invoke([this, widget] {
                if (on_double_click)
                    on_double_click(*widget);
            });
        }
        return;
    }
}

void TabWidget::set_container_margins(GUI::Margins const& margins)
{
    m_container_margins = margins;
    layout_relevant_change_occurred();
    update();
}

Optional<UISize> TabWidget::calculated_min_size() const
{
    if (!m_active_widget)
        return {};
    auto content_min_size = m_active_widget->effective_min_size();
    UIDimension width = MUST(content_min_size.width().shrink_value()), height = MUST(content_min_size.height().shrink_value());
    width.add_if_int(container_margins().vertical_total()
        + (first_is_one_of(m_tab_position, TabPosition::Left, TabPosition::Right) ? bar_rect().width() : 0));
    height.add_if_int(container_margins().vertical_total()
        + (first_is_one_of(m_tab_position, TabPosition::Top, TabPosition::Bottom) ? bar_rect().height() : 0));

    return UISize { width, height };
}

Optional<UISize> TabWidget::calculated_preferred_size() const
{
    if (!m_active_widget)
        return {};
    auto content_preferred_size = m_active_widget->effective_preferred_size();
    UIDimension width = MUST(content_preferred_size.width().shrink_value()), height = MUST(content_preferred_size.height().shrink_value());
    width.add_if_int(container_margins().vertical_total()
        + (first_is_one_of(m_tab_position, TabPosition::Left, TabPosition::Right) ? bar_rect().width() : 0));
    height.add_if_int(
        container_margins().vertical_total()
        + (first_is_one_of(m_tab_position, TabPosition::Top, TabPosition::Bottom) ? bar_rect().height() : 0));
    return UISize { width, height };
}

void TabWidget::drag_tab(size_t index)
{
    m_dragging_active_tab = m_reorder_allowed;
    m_grab_offset = m_mouse_pos - (this->has_vertical_tabs() ? button_rect(index).y() : button_rect(index).x());
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
        auto button_pos = this->has_vertical_tabs() ? (this->button_rect(target)).y() : (this->button_rect(target)).x();
        if (m_mouse_pos - m_grab_offset < button_pos) {
            break;
        }
    }

    if (target == active) {
        for (target = m_tabs.size() - 1; target > active; --target) {
            auto button_pos = this->has_vertical_tabs() ? (this->button_rect(target)).y() : (this->button_rect(target)).x();
            if (m_mouse_pos - m_grab_offset > button_pos) {
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
