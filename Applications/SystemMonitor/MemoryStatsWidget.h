#pragma once

#include <LibCore/CFile.h>
#include <LibGUI/GWidget.h>

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
    ObjectPtr<GLabel> m_user_physical_pages_label;
    ObjectPtr<GLabel> m_supervisor_physical_pages_label;
    ObjectPtr<GLabel> m_kmalloc_label;
    ObjectPtr<GLabel> m_kmalloc_count_label;
    CFile m_proc_memstat;
};
