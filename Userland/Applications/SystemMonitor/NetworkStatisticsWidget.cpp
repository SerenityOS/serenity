/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NetworkStatisticsWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>

NetworkStatisticsWidget::NetworkStatisticsWidget()
{
    on_first_show = [this](auto&) {
        set_layout<GUI::VerticalBoxLayout>();
        layout()->set_margins({ 4, 4, 4, 4 });
        set_fill_with_background_color(true);

        auto& adapters_group_box = add<GUI::GroupBox>("Adapters");
        adapters_group_box.set_layout<GUI::VerticalBoxLayout>();
        adapters_group_box.layout()->set_margins({ 6, 16, 6, 6 });
        adapters_group_box.set_fixed_height(120);

        m_adapter_table_view = adapters_group_box.add<GUI::TableView>();

        Vector<GUI::JsonArrayModel::FieldSpec> net_adapters_fields;
        net_adapters_fields.empend("name", "Name", Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("class_name", "Class", Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("mac_address", "MAC", Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("ipv4_address", "IPv4", Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("packets_in", "Pkt In", Gfx::TextAlignment::CenterRight);
        net_adapters_fields.empend("packets_out", "Pkt Out", Gfx::TextAlignment::CenterRight);
        net_adapters_fields.empend("bytes_in", "Bytes In", Gfx::TextAlignment::CenterRight);
        net_adapters_fields.empend("bytes_out", "Bytes Out", Gfx::TextAlignment::CenterRight);
        m_adapter_model = GUI::JsonArrayModel::create("/proc/net/adapters", move(net_adapters_fields));
        m_adapter_table_view->set_model(GUI::SortingProxyModel::create(*m_adapter_model));

        auto& sockets_group_box = add<GUI::GroupBox>("Sockets");
        sockets_group_box.set_layout<GUI::VerticalBoxLayout>();
        sockets_group_box.layout()->set_margins({ 6, 16, 6, 6 });

        m_tcp_socket_table_view = sockets_group_box.add<GUI::TableView>();

        Vector<GUI::JsonArrayModel::FieldSpec> net_tcp_fields;
        net_tcp_fields.empend("peer_address", "Peer", Gfx::TextAlignment::CenterLeft);
        net_tcp_fields.empend("peer_port", "Port", Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("local_address", "Local", Gfx::TextAlignment::CenterLeft);
        net_tcp_fields.empend("local_port", "Port", Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("state", "State", Gfx::TextAlignment::CenterLeft);
        net_tcp_fields.empend("ack_number", "Ack#", Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("sequence_number", "Seq#", Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("packets_in", "Pkt In", Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("packets_out", "Pkt Out", Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("bytes_in", "Bytes In", Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("bytes_out", "Bytes Out", Gfx::TextAlignment::CenterRight);
        m_tcp_socket_model = GUI::JsonArrayModel::create("/proc/net/tcp", move(net_tcp_fields));
        m_tcp_socket_table_view->set_model(GUI::SortingProxyModel::create(*m_tcp_socket_model));

        m_udp_socket_table_view = sockets_group_box.add<GUI::TableView>();

        Vector<GUI::JsonArrayModel::FieldSpec> net_udp_fields;
        net_udp_fields.empend("peer_address", "Peer", Gfx::TextAlignment::CenterLeft);
        net_udp_fields.empend("peer_port", "Port", Gfx::TextAlignment::CenterRight);
        net_udp_fields.empend("local_address", "Local", Gfx::TextAlignment::CenterLeft);
        net_udp_fields.empend("local_port", "Port", Gfx::TextAlignment::CenterRight);
        m_udp_socket_model = GUI::JsonArrayModel::create("/proc/net/udp", move(net_udp_fields));
        m_udp_socket_table_view->set_model(GUI::SortingProxyModel::create(*m_udp_socket_model));

        m_update_timer = add<Core::Timer>(
            1000, [this] {
                update_models();
            });

        update_models();
    };
}

NetworkStatisticsWidget::~NetworkStatisticsWidget()
{
}

void NetworkStatisticsWidget::update_models()
{
    m_adapter_table_view->model()->update();
    m_tcp_socket_table_view->model()->update();
    m_udp_socket_table_view->model()->update();
}
