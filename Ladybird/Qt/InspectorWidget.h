/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WebContentView.h"
#include <LibWebView/Forward.h>
#include <QWidget>

namespace Ladybird {

class WebContentView;

class InspectorWidget final : public QWidget {
    Q_OBJECT

public:
    InspectorWidget(QWidget* tab, WebContentView& content_view);
    virtual ~InspectorWidget() override;

    void inspect();
    void reset();

    void select_hovered_node();
    void select_default_node();

private:
    void closeEvent(QCloseEvent*) override;

    WebContentView* m_inspector_view;
    OwnPtr<WebView::InspectorClient> m_inspector_client;
};

}
