#pragma once

#include <LibGUI/GWidget.h>

class GTableView;

class ProcessMemoryMapWidget final : public GWidget {
    C_OBJECT(ProcessMemoryMapWidget);
public:
    virtual ~ProcessMemoryMapWidget() override;

    void set_pid(pid_t);

private:
    explicit ProcessMemoryMapWidget(GWidget* parent);
    RefPtr<GTableView> m_table_view;
    pid_t m_pid { -1 };
};
