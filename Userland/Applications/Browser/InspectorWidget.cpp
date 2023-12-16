/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectorWidget.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Menu.h>
#include <LibWebView/Attribute.h>
#include <LibWebView/InspectorClient.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace Browser {

NonnullRefPtr<InspectorWidget> InspectorWidget::create(WebView::OutOfProcessWebView& content_view)
{
    return adopt_ref(*new (nothrow) InspectorWidget(content_view));
}

InspectorWidget::InspectorWidget(WebView::OutOfProcessWebView& content_view)
{
    set_layout<GUI::VerticalBoxLayout>(4);
    set_fill_with_background_color(true);

    m_inspector_view = add<WebView::OutOfProcessWebView>();
    m_inspector_client = make<WebView::InspectorClient>(content_view, *m_inspector_view);

    m_edit_node_action = GUI::Action::create("&Edit node"sv, [this](auto&) { m_inspector_client->context_menu_edit_dom_node(); });
    m_copy_node_action = GUI::Action::create("&Copy HTML"sv, [this](auto&) { m_inspector_client->context_menu_copy_dom_node(); });
    m_screenshot_node_action = GUI::Action::create("Take node &screenshot"sv, [this](auto&) { m_inspector_client->context_menu_screenshot_dom_node(); });
    m_create_child_element_action = GUI::Action::create("Create child &element"sv, [this](auto&) { m_inspector_client->context_menu_create_child_element(); });
    m_create_child_text_node_action = GUI::Action::create("Create child &text node"sv, [this](auto&) { m_inspector_client->context_menu_create_child_text_node(); });
    m_clone_node_action = GUI::Action::create("C&lone node"sv, [this](auto&) { m_inspector_client->context_menu_clone_dom_node(); });
    m_delete_node_action = GUI::Action::create("&Delete node"sv, [this](auto&) { m_inspector_client->context_menu_remove_dom_node(); });
    m_add_attribute_action = GUI::Action::create("&Add attribute"sv, [this](auto&) { m_inspector_client->context_menu_add_dom_node_attribute(); });
    m_remove_attribute_action = GUI::Action::create("&Remove attribute"sv, [this](auto&) { m_inspector_client->context_menu_remove_dom_node_attribute(); });
    m_copy_attribute_value_action = GUI::Action::create("Copy attribute &value"sv, [this](auto&) { m_inspector_client->context_menu_copy_dom_node_attribute_value(); });

    auto add_create_child_menu = [&](auto& menu) {
        auto create_child_menu = menu.add_submenu("Create child"_string);
        create_child_menu->add_action(*m_create_child_element_action);
        create_child_menu->add_action(*m_create_child_text_node_action);
    };

    m_dom_node_text_context_menu = GUI::Menu::construct();
    m_dom_node_text_context_menu->add_action(*m_edit_node_action);
    m_dom_node_text_context_menu->add_action(*m_copy_node_action);
    m_dom_node_text_context_menu->add_separator();
    m_dom_node_text_context_menu->add_action(*m_delete_node_action);

    m_dom_node_tag_context_menu = GUI::Menu::construct();
    m_dom_node_tag_context_menu->add_action(*m_edit_node_action);
    m_dom_node_tag_context_menu->add_separator();
    m_dom_node_tag_context_menu->add_action(*m_add_attribute_action);
    add_create_child_menu(*m_dom_node_tag_context_menu);
    m_dom_node_tag_context_menu->add_action(*m_clone_node_action);
    m_dom_node_tag_context_menu->add_action(*m_delete_node_action);
    m_dom_node_tag_context_menu->add_separator();
    m_dom_node_tag_context_menu->add_action(*m_copy_node_action);
    m_dom_node_tag_context_menu->add_action(*m_screenshot_node_action);

    m_dom_node_attribute_context_menu = GUI::Menu::construct();
    m_dom_node_attribute_context_menu->add_action(*m_edit_node_action);
    m_dom_node_attribute_context_menu->add_action(*m_copy_attribute_value_action);
    m_dom_node_attribute_context_menu->add_action(*m_remove_attribute_action);
    m_dom_node_attribute_context_menu->add_separator();
    m_dom_node_attribute_context_menu->add_action(*m_add_attribute_action);
    add_create_child_menu(*m_dom_node_attribute_context_menu);
    m_dom_node_attribute_context_menu->add_action(*m_clone_node_action);
    m_dom_node_attribute_context_menu->add_action(*m_delete_node_action);
    m_dom_node_attribute_context_menu->add_separator();
    m_dom_node_attribute_context_menu->add_action(*m_copy_node_action);
    m_dom_node_attribute_context_menu->add_action(*m_screenshot_node_action);

    m_inspector_client->on_requested_dom_node_text_context_menu = [this](auto position) {
        m_edit_node_action->set_text("&Edit text");
        m_copy_node_action->set_text("&Copy text");

        m_dom_node_text_context_menu->popup(to_widget_position(position));
    };

    m_inspector_client->on_requested_dom_node_tag_context_menu = [this](auto position, auto const& tag) {
        m_edit_node_action->set_text(ByteString::formatted("&Edit \"{}\"", tag));
        m_copy_node_action->set_text("&Copy HTML");

        m_dom_node_tag_context_menu->popup(to_widget_position(position));
    };

    m_inspector_client->on_requested_dom_node_attribute_context_menu = [this](auto position, auto const&, auto const& attribute) {
        static constexpr size_t MAX_ATTRIBUTE_VALUE_LENGTH = 32;

        m_copy_node_action->set_text("&Copy HTML");
        m_edit_node_action->set_text(ByteString::formatted("&Edit attribute \"{}\"", attribute.name));
        m_remove_attribute_action->set_text(ByteString::formatted("&Remove attribute \"{}\"", attribute.name));
        m_copy_attribute_value_action->set_text(ByteString::formatted("Copy attribute &value \"{:.{}}{}\"",
            attribute.value, MAX_ATTRIBUTE_VALUE_LENGTH,
            attribute.value.bytes_as_string_view().length() > MAX_ATTRIBUTE_VALUE_LENGTH ? "..."sv : ""sv));

        m_dom_node_attribute_context_menu->popup(to_widget_position(position));
    };

    m_inspector_view->set_focus(true);
}

InspectorWidget::~InspectorWidget() = default;

void InspectorWidget::inspect()
{
    m_inspector_client->inspect();
}

void InspectorWidget::reset()
{
    m_inspector_client->reset();
}

void InspectorWidget::select_default_node()
{
    m_inspector_client->select_default_node();
}

void InspectorWidget::select_hovered_node()
{
    m_inspector_client->select_hovered_node();
}

Gfx::IntPoint InspectorWidget::to_widget_position(Gfx::IntPoint position) const
{
    return m_inspector_view->screen_relative_rect().location().translated(position);
}

}
