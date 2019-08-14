#pragma once

#include <LibGUI/GWidget.h>

class GTableView;

class ProcessFileDescriptorMapWidget final : public GWidget {
    C_OBJECT(ProcessFileDescriptorMapWidget);
public:
    explicit ProcessFileDescriptorMapWidget(GWidget* parent);
    virtual ~ProcessFileDescriptorMapWidget() override;

    void set_pid(pid_t);

private:
    GTableView* m_table_view { nullptr };
    pid_t m_pid { -1 };
};
