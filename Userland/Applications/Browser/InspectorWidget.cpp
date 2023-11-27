/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectorWidget.h"
#include <LibGUI/BoxLayout.h>
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

}
