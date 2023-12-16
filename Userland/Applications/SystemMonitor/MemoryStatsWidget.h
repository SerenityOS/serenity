/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace SystemMonitor {

class GraphWidget;

class MemoryStatsWidget final : public GUI::Widget {
    C_OBJECT(MemoryStatsWidget)
public:
    static MemoryStatsWidget* the();

    virtual ~MemoryStatsWidget() override = default;

    void set_graph_widget(GraphWidget& graph);

    void set_graph_widget_via_name(ByteString name);
    ByteString graph_widget_name();

    void refresh();

private:
    MemoryStatsWidget(GraphWidget* graph);
    MemoryStatsWidget();

    GraphWidget* m_graph;
    // Is null if we have a valid graph
    ByteString m_graph_widget_name {};
    RefPtr<GUI::Label> m_physical_pages_label;
    RefPtr<GUI::Label> m_physical_pages_committed_label;
    RefPtr<GUI::Label> m_kmalloc_space_label;
    RefPtr<GUI::Label> m_kmalloc_count_label;
    RefPtr<GUI::Label> m_kfree_count_label;
    RefPtr<GUI::Label> m_kmalloc_difference_label;
};

}
