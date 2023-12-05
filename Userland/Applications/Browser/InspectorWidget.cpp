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
    m_delete_node_action = GUI::Action::create("&Delete node"sv, [this](auto&) { m_inspector_client->context_menu_remove_dom_node(); });
    m_add_attribute_action = GUI::Action::create("&Add attribute"sv, [this](auto&) { m_inspector_client->context_menu_add_dom_node_attribute(); });
    m_remove_attribute_action = GUI::Action::create("&Remove attribute"sv, [this](auto&) { m_inspector_client->context_menu_remove_dom_node_attribute(); });

    m_dom_node_text_context_menu = GUI::Menu::construct();
    m_dom_node_text_context_menu->add_action(*m_edit_node_action);
    m_dom_node_text_context_menu->add_separator();
    m_dom_node_text_context_menu->add_action(*m_delete_node_action);

    m_dom_node_tag_context_menu = GUI::Menu::construct();
    m_dom_node_tag_context_menu->add_action(*m_edit_node_action);
    m_dom_node_tag_context_menu->add_separator();
    m_dom_node_tag_context_menu->add_action(*m_add_attribute_action);
    m_dom_node_tag_context_menu->add_action(*m_delete_node_action);

    m_dom_node_attribute_context_menu = GUI::Menu::construct();
    m_dom_node_attribute_context_menu->add_action(*m_edit_node_action);
    m_dom_node_attribute_context_menu->add_action(*m_remove_attribute_action);
    m_dom_node_attribute_context_menu->add_separator();
    m_dom_node_attribute_context_menu->add_action(*m_add_attribute_action);
    m_dom_node_attribute_context_menu->add_action(*m_delete_node_action);

    m_inspector_client->on_requested_dom_node_text_context_menu = [this](auto position) {
        m_edit_node_action->set_text("&Edit text");

        m_dom_node_text_context_menu->popup(to_widget_position(position));
    };

    m_inspector_client->on_requested_dom_node_tag_context_menu = [this](auto position, auto const& tag) {
        m_edit_node_action->set_text(DeprecatedString::formatted("&Edit \"{}\"", tag));

        m_dom_node_tag_context_menu->popup(to_widget_position(position));
    };

    m_inspector_client->on_requested_dom_node_attribute_context_menu = [this](auto position, auto const& attribute) {
        m_edit_node_action->set_text(DeprecatedString::formatted("&Edit attribute \"{}\"", attribute));
        m_remove_attribute_action->set_text(DeprecatedString::formatted("&Remove attribute \"{}\"", attribute));

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
