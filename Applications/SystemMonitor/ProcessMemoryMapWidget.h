#pragma once

#include <LibGUI/GWidget.h>

class GTableView;

class ProcessMemoryMapWidget final : public GWidget {
    C_OBJECT(ProcessMemoryMapWidget);
public:
    explicit ProcessMemoryMapWidget(GWidget* parent);
    virtual ~ProcessMemoryMapWidget() override;

    void set_pid(pid_t);

private:
    GTableView* m_table_view { nullptr };
    pid_t m_pid { -1 };
};
