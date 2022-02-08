/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/RefPtr.h>
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

bool ScrollableContainerWidget::load_from_gml_ast(NonnullRefPtr<GUI::GML::Node> ast, RefPtr<Core::Object> (*unregistered_child_handler)(const String&))
{
    if (is<GUI::GML::GMLFile>(ast.ptr()))
        return load_from_gml_ast(static_ptr_cast<GUI::GML::GMLFile>(ast)->main_class(), unregistered_child_handler);

    VERIFY(is<GUI::GML::Object>(ast.ptr()));
    auto object = static_ptr_cast<GUI::GML::Object>(ast);

    object->for_each_property([&](auto key, auto value) {
        set_property(key, value);
    });

    auto content_widget_value = object->get_property("content_widget"sv);
    if (!content_widget_value.is_null() && !is<Object>(content_widget_value.ptr())) {
        dbgln("content widget is not an object");
        return false;
    }

    auto has_children = false;
    object->for_each_child_object([&](auto) { has_children = true; });
    if (has_children) {
        dbgln("children specified for ScrollableContainerWidget, but only 1 widget as content_widget is supported");
        return false;
    }

    if (!content_widget_value.is_null() && is<Object>(content_widget_value.ptr())) {
        auto content_widget = static_ptr_cast<GUI::GML::Object>(content_widget_value);
        auto class_name = content_widget->name();

        RefPtr<Core::Object> child;
        if (auto* registration = Core::ObjectClassRegistration::find(class_name)) {
            child = registration->construct();
        } else {
            child = unregistered_child_handler(class_name);
        }
        if (!child)
            return false;
        auto widget_ptr = verify_cast<GUI::Widget>(child.ptr());
        set_widget(widget_ptr);
        static_ptr_cast<Widget>(child)->load_from_gml_ast(content_widget.release_nonnull(), unregistered_child_handler);
        return true;
    }

    return true;
}

}
