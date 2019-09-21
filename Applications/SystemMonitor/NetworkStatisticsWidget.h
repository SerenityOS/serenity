#pragma once

#include <LibCore/CTimer.h>
#include <LibGUI/GWidget.h>

class GTableView;

class NetworkStatisticsWidget final : public GWidget {
    C_OBJECT(NetworkStatisticsWidget)
public:
    virtual ~NetworkStatisticsWidget() override;

private:
    explicit NetworkStatisticsWidget(GWidget* parent = nullptr);
    void update_models();

    RefPtr<GTableView> m_adapter_table_view;
    RefPtr<GTableView> m_socket_table_view;
    RefPtr<CTimer> m_update_timer;
};
