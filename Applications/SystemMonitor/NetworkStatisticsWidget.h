#pragma once

#include <LibCore/CTimer.h>
#include <LibGUI/GWidget.h>

class GTableView;

class NetworkStatisticsWidget final : public GWidget {
    C_OBJECT(NetworkStatisticsWidget)
public:
    explicit NetworkStatisticsWidget(GWidget* parent = nullptr);
    virtual ~NetworkStatisticsWidget() override;

private:
    void update_models();

    GTableView* m_adapter_table_view { nullptr };
    GTableView* m_socket_table_view { nullptr };
    CTimer* m_update_timer { nullptr };
};
