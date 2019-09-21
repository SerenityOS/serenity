#pragma once

#include <LibGUI/GWidget.h>

class GTableView;

class ProcessFileDescriptorMapWidget final : public GWidget {
    C_OBJECT(ProcessFileDescriptorMapWidget);
public:
    virtual ~ProcessFileDescriptorMapWidget() override;

    void set_pid(pid_t);

private:
    explicit ProcessFileDescriptorMapWidget(GWidget* parent);

    ObjectPtr<GTableView> m_table_view;
    pid_t m_pid { -1 };
};
