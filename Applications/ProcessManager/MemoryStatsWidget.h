#pragma once

#include <LibGUI/GWidget.h>

class GLabel;

class MemoryStatsWidget final : public GWidget {
public:
    explicit MemoryStatsWidget(GWidget* parent);
    virtual ~MemoryStatsWidget() override;

    void refresh();

private:
    virtual void timer_event(CTimerEvent&) override;
    virtual void paint_event(GPaintEvent&) override;

    GLabel* m_user_physical_pages_label { nullptr };
    GLabel* m_supervisor_physical_pages_label { nullptr };
    GLabel* m_kmalloc_label { nullptr };
    GLabel* m_kmalloc_count_label { nullptr };
};
