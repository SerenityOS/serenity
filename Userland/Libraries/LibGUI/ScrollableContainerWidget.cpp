/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Layout.h>
#include <LibGUI/ScrollableContainerWidget.h>

REGISTER_WIDGET(GUI, ScrollableContainerWidget)

namespace GUI {

ScrollableContainerWidget::ScrollableContainerWidget()
{
    REGISTER_BOOL_PROPERTY("scrollbars_enabled", is_scrollbars_enabled, set_scrollbars_enabled);
    REGISTER_BOOL_PROPERTY("should_hide_unnecessary_scrollbars", should_hide_unnecessary_scrollbars, set_should_hide_unnecessary_scrollbars);
}

ScrollableContainerWidget::~ScrollableContainerWidget()
{
}

void ScrollableContainerWidget::did_scroll()
{
    AbstractScrollableWidget::did_scroll();
    update_widget_position();
}

void ScrollableContainerWidget::update_widget_position()
{
    if (!m_widget)
        return;
    m_widget->move_to(-horizontal_scrollbar().value() + content_margins().left(), -vertical_scrollbar().value() + content_margins().top());
}

void ScrollableContainerWidget::update_widget_size()
{
    if (!m_widget)
        return;
    m_widget->do_layout();
    if (m_widget->is_shrink_to_fit() && m_widget->layout()) {
        auto new_size = Widget::content_size();
        auto preferred_size = m_widget->layout()->preferred_size();
        if (preferred_size.width() != -1)
            new_size.set_width(preferred_size.width());
        if (preferred_size.height() != -1)
            new_size.set_height(preferred_size.height());
        m_widget->resize(new_size);
        set_content_size(new_size);
    } else {
        auto inner_size = Widget::content_size();
        auto min_size = m_widget->min_size();
        auto new_size = Gfx::Size {
            max(inner_size.width(), min_size.width()),
            max(inner_size.height(), min_size.height())
        };
        m_widget->resize(new_size);
        set_content_size(new_size);
    }
}

void ScrollableContainerWidget::resize_event(GUI::ResizeEvent& event)
{
    AbstractScrollableWidget::resize_event(event);
    update_widget_position();
    update_widget_size();
}

void ScrollableContainerWidget::set_widget(GUI::Widget* widget)
{
    if (m_widget == widget)
        return;

    if (m_widget)
        remove_child(*m_widget);

    m_widget = widget;

    if (m_widget) {
        add_child(*m_widget);
        m_widget->move_to_back();
    }
    update_widget_size();
    update_widget_position();
}

bool ScrollableContainerWidget::load_from_json(const JsonObject& json, RefPtr<Core::Object> (*unregistered_child_handler)(const String&))
{
    json.for_each_member([&](auto& key, auto& value) {
        set_property(key, value);
    });

    auto layout_value = json.get("layout");
    if (!layout_value.is_null()) {
        dbgln("Layout specified in ScrollableContainerWidget, this is not supported.");
        return false;
    }

    auto content_widget_value = json.get("content_widget");
    if (!content_widget_value.is_null() && !content_widget_value.is_object()) {
        dbgln("content widget is not an object");
        return false;
    }

    if (!json.get("children").is_null()) {
        dbgln("children specified for ScrollableContainerWidget, but only 1 widget as content_widget is supported");
        return false;
    }

    if (content_widget_value.is_object()) {
        auto& content_widget = content_widget_value.as_object();
        auto class_name = content_widget.get("class");
        if (!class_name.is_string()) {
            dbgln("Invalid content widget class name");
            return false;
        }

        RefPtr<Core::Object> child;
        if (auto* registration = Core::ObjectClassRegistration::find(class_name.as_string())) {
            child = registration->construct();
        } else {
            child = unregistered_child_handler(class_name.as_string());
        }
        if (!child)
            return false;
        auto widget_ptr = verify_cast<GUI::Widget>(child.ptr());
        set_widget(widget_ptr);
        child->load_from_json(content_widget, unregistered_child_handler);
        return true;
    }

    return true;
}

}
