#pragma once

#include <LibGUI/GWidget.h>

class CTimer;
class GJsonArrayModel;
class GTableView;

class ProcessMemoryMapWidget final : public GWidget {
    C_OBJECT(ProcessMemoryMapWidget);
public:
    virtual ~ProcessMemoryMapWidget() override;

    void set_pid(pid_t);
    void refresh();

private:
    explicit ProcessMemoryMapWidget(GWidget* parent);
    RefPtr<GTableView> m_table_view;
    RefPtr<GJsonArrayModel> m_json_model;
    pid_t m_pid { -1 };
    RefPtr<CTimer> m_timer;
};
