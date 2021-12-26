#pragma once

#include <LibGUI/GWidget.h>
#include <LibCore/CFile.h>

class GLabel;
class GraphWidget;

class MemoryStatsWidget final : public GWidget {
public:
    MemoryStatsWidget(GraphWidget& graph, GWidget* parent);
    virtual ~MemoryStatsWidget() override;

    void refresh();

private:
    virtual void timer_event(CTimerEvent&) override;

    GraphWidget& m_graph;
    GLabel* m_user_physical_pages_label { nullptr };
    GLabel* m_supervisor_physical_pages_label { nullptr };
    GLabel* m_kmalloc_label { nullptr };
    GLabel* m_kmalloc_count_label { nullptr };
    CFile m_proc_memstat;
};
