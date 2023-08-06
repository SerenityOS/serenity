/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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
        auto preferred_size = m_widget->effective_preferred_size();
        if (preferred_size.width().is_int())
            new_size.set_width(preferred_size.width().as_int());
        if (preferred_size.height().is_int())
            new_size.set_height(preferred_size.height().as_int());
        m_widget->resize(new_size);
        set_content_size(new_size);
    } else {
        auto inner_size = Widget::content_size();
        auto min_size = m_widget->effective_min_size();
        auto new_size = Gfx::Size {
            max(inner_size.width(), MUST(min_size.width().shrink_value())),
            max(inner_size.height(), MUST(min_size.height().shrink_value()))
        };
        m_widget->resize(new_size);
        set_content_size(new_size);
    }
}

void ScrollableContainerWidget::update_widget_min_size()
{
    if (!m_widget)
        set_min_content_size({});
    else
        set_min_content_size(Gfx::IntSize(m_widget->effective_min_size().replace_component_if_matching_with(SpecialDimension::Shrink, UISize { 0, 0 })));
}

void ScrollableContainerWidget::resize_event(GUI::ResizeEvent& event)
{
    AbstractScrollableWidget::resize_event(event);
    update_widget_size();
    update_widget_position();
}

void ScrollableContainerWidget::layout_relevant_change_occurred()
{
    update_widget_min_size();
    update_scrollbar_visibility();
    update_scrollbar_ranges();
    update_widget_size();
    update_widget_position();
    update();
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
    update_widget_min_size();
    update_widget_size();
    update_widget_position();
}

ErrorOr<void> ScrollableContainerWidget::load_from_gml_ast(NonnullRefPtr<GUI::GML::Node const> ast, UnregisteredChildHandler unregistered_child_handler)
{
    if (is<GUI::GML::GMLFile>(ast.ptr()))
        return load_from_gml_ast(static_cast<GUI::GML::GMLFile const&>(*ast).main_class(), unregistered_child_handler);

    VERIFY(is<GUI::GML::Object>(ast.ptr()));
    auto const& object = static_cast<GUI::GML::Object const&>(*ast);

    object.for_each_property([&](auto key, auto value) {
        set_property(key, value);
    });

    auto content_widget_value = object.get_property("content_widget"sv);
    if (!content_widget_value.is_null() && !is<GUI::GML::Object>(content_widget_value.ptr())) {
        return Error::from_string_literal("ScrollableContainerWidget content_widget is not an object");
    }

    auto has_children = false;
    object.for_each_child_object([&](auto) { has_children = true; });
    if (has_children) {
        return Error::from_string_literal("Children specified for ScrollableContainerWidget, but only 1 widget as content_widget is supported");
    }

    if (!content_widget_value.is_null() && is<GUI::GML::Object>(content_widget_value.ptr())) {
        auto const& content_widget = static_cast<GUI::GML::Object const&>(*content_widget_value);
        auto class_name = content_widget.name();

        RefPtr<Core::EventReceiver> child;
        if (auto* registration = GUI::ObjectClassRegistration::find(class_name)) {
            child = TRY(registration->construct());
        } else {
            child = TRY(unregistered_child_handler(class_name));
        }
        if (!child)
            return Error::from_string_literal("Unable to construct a Widget class for ScrollableContainerWidget content_widget property");
        auto widget_ptr = verify_cast<GUI::Widget>(child.ptr());
        set_widget(widget_ptr);
        TRY(static_cast<Widget&>(*child).load_from_gml_ast(content_widget, unregistered_child_handler));
    }

    return {};
}

}
