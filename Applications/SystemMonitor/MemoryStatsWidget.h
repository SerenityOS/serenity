#pragma once

#include <LibGUI/GWidget.h>

class GLabel;
class GraphWidget;

class MemoryStatsWidget final : public GWidget {
    C_OBJECT(MemoryStatsWidget)
public:
    static MemoryStatsWidget* the();

    virtual ~MemoryStatsWidget() override;

    void refresh();

private:
    MemoryStatsWidget(GraphWidget& graph, GWidget* parent);

    GraphWidget& m_graph;
    RefPtr<GLabel> m_user_physical_pages_label;
    RefPtr<GLabel> m_supervisor_physical_pages_label;
    RefPtr<GLabel> m_kmalloc_label;
    RefPtr<GLabel> m_kmalloc_count_label;
};
