#pragma once

#include <LibCore/CFile.h>
#include <LibGUI/GWidget.h>

class GLabel;
class GraphWidget;

class MemoryStatsWidget final : public GWidget {
    C_OBJECT(MemoryStatsWidget)
public:
    MemoryStatsWidget(GraphWidget& graph, GWidget* parent);
    virtual ~MemoryStatsWidget() override;

    void refresh();

private:
    virtual void timer_event(CTimerEvent&) override;

    GraphWidget& m_graph;
    RefPtr<GLabel> m_user_physical_pages_label;
    RefPtr<GLabel> m_supervisor_physical_pages_label;
    RefPtr<GLabel> m_kmalloc_label;
    RefPtr<GLabel> m_kmalloc_count_label;
    RefPtr<CFile> m_proc_memstat;
};
