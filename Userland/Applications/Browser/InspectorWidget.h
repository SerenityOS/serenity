/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ElementSizePreviewWidget.h"
#include <AK/StringView.h>
#include <LibGUI/Widget.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/BoxModelMetrics.h>
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

    void load_style_json(StringView computed_values_json, StringView resolved_values_json, StringView custom_properties_json);
    void clear_style_json();

    void update_node_box_model(StringView node_box_sizing_json);
    void clear_node_box_model();

    void update_aria_properties_state_model(StringView aria_properties_state_json);

    RefPtr<WebView::OutOfProcessWebView> m_inspector_view;
    OwnPtr<WebView::InspectorClient> m_inspector_client;

    RefPtr<GUI::TableView> m_computed_style_table_view;
    RefPtr<GUI::TableView> m_resolved_style_table_view;
    RefPtr<GUI::TableView> m_custom_properties_table_view;
    RefPtr<GUI::TableView> m_aria_properties_state_view;
    RefPtr<ElementSizePreviewWidget> m_element_size_view;

    Web::Layout::BoxModelMetrics m_node_box_sizing;
};

}
