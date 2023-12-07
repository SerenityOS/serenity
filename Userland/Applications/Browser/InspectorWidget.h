/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibWeb/Forward.h>
#include <LibWebView/Forward.h>

namespace Browser {

class InspectorWidget final : public GUI::Widget {
    C_OBJECT(InspectorWidget)

public:
    static NonnullRefPtr<InspectorWidget> create(WebView::OutOfProcessWebView& content_view);
    virtual ~InspectorWidget();

    void inspect();
    void reset();

    void select_default_node();
    void select_hovered_node();

private:
    explicit InspectorWidget(WebView::OutOfProcessWebView& content_view);

    Gfx::IntPoint to_widget_position(Gfx::IntPoint) const;

    RefPtr<WebView::OutOfProcessWebView> m_inspector_view;
    OwnPtr<WebView::InspectorClient> m_inspector_client;

    RefPtr<GUI::Menu> m_dom_node_text_context_menu;
    RefPtr<GUI::Menu> m_dom_node_tag_context_menu;
    RefPtr<GUI::Menu> m_dom_node_attribute_context_menu;

    RefPtr<GUI::Action> m_edit_node_action;
    RefPtr<GUI::Action> m_copy_node_action;
    RefPtr<GUI::Action> m_screenshot_node_action;
    RefPtr<GUI::Action> m_create_child_element_action;
    RefPtr<GUI::Action> m_create_child_text_node_action;
    RefPtr<GUI::Action> m_clone_node_action;
    RefPtr<GUI::Action> m_delete_node_action;
    RefPtr<GUI::Action> m_add_attribute_action;
    RefPtr<GUI::Action> m_remove_attribute_action;
    RefPtr<GUI::Action> m_copy_attribute_value_action;
};

}
