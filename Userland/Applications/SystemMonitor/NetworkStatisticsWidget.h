/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/LazyWidget.h>

namespace SystemMonitor {

class NetworkStatisticsWidget final : public GUI::LazyWidget {
    C_OBJECT(NetworkStatisticsWidget)
public:
    virtual ~NetworkStatisticsWidget() override = default;

private:
    NetworkStatisticsWidget();
    void update_models();

    RefPtr<GUI::TableView> m_adapter_table_view;
    RefPtr<GUI::Menu> m_adapter_context_menu;
    RefPtr<GUI::TableView> m_tcp_socket_table_view;
    RefPtr<GUI::TableView> m_udp_socket_table_view;
    RefPtr<GUI::JsonArrayModel> m_adapter_model;
    RefPtr<GUI::JsonArrayModel> m_tcp_socket_model;
    RefPtr<GUI::JsonArrayModel> m_udp_socket_model;
    RefPtr<Core::Timer> m_update_timer;
    RefPtr<Gfx::Bitmap> m_network_connected_bitmap;
    RefPtr<Gfx::Bitmap> m_network_disconnected_bitmap;
    RefPtr<Gfx::Bitmap> m_network_link_down_bitmap;
};

}
