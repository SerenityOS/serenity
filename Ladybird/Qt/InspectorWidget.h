/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ModelAdapter.h"
#include "WebContentView.h"
#include <AK/StringView.h>
#include <LibWebView/Forward.h>
#include <QWidget>

class QTableView;

namespace Ladybird {

class WebContentView;

class InspectorWidget final : public QWidget {
    Q_OBJECT

public:
    explicit InspectorWidget(WebContentView& content_view);
    virtual ~InspectorWidget() override;

    void inspect();
    void reset();

    void select_hovered_node();
    void select_default_node();

private:
    void load_style_json(StringView computed_style_json, StringView resolved_style_json, StringView custom_properties_json);
    void clear_style_json();

    void closeEvent(QCloseEvent*) override;

    OwnPtr<WebContentView> m_inspector_view;
    OwnPtr<WebView::InspectorClient> m_inspector_client;

    OwnPtr<PropertyTableModel> m_computed_style_model;
    OwnPtr<PropertyTableModel> m_resolved_style_model;
    OwnPtr<PropertyTableModel> m_custom_properties_model;

    QTableView* m_computed_style_table { nullptr };
    QTableView* m_resolved_style_table { nullptr };
    QTableView* m_custom_properties_table { nullptr };
};

}
