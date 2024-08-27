/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NetworkStatisticsWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Process.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>
#include <LibGfx/Painter.h>

REGISTER_WIDGET(SystemMonitor, NetworkStatisticsWidget)

namespace SystemMonitor {

NetworkStatisticsWidget::NetworkStatisticsWidget()
{
    on_first_show = [this](auto&) {
        set_layout<GUI::VerticalBoxLayout>(4);
        set_fill_with_background_color(true);

        m_network_connected_bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/network-connected.png"sv).release_value_but_fixme_should_propagate_errors();
        m_network_disconnected_bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/network-disconnected.png"sv).release_value_but_fixme_should_propagate_errors();

        m_network_link_down_bitmap = Gfx::Bitmap::create(m_network_connected_bitmap->format(), m_network_connected_bitmap->size()).release_value_but_fixme_should_propagate_errors();
        {
            Gfx::Painter painter(*m_network_link_down_bitmap);
            painter.blit_filtered(Gfx::IntPoint {}, *m_network_connected_bitmap, m_network_connected_bitmap->rect(), [](Color color) {
                return color.to_grayscale();
            });
        }

        auto& adapters_group_box = add<GUI::GroupBox>("Adapters"sv);
        adapters_group_box.set_layout<GUI::VerticalBoxLayout>(6);
        adapters_group_box.set_fixed_height(120);

        m_adapter_table_view = adapters_group_box.add<GUI::TableView>();

        Vector<GUI::JsonArrayModel::FieldSpec> net_adapters_fields;
        net_adapters_fields.empend(String(), Gfx::TextAlignment::CenterLeft,
            [this](JsonObject const& object) -> GUI::Variant {
                if (!object.get_bool("link_up"sv).value_or(false))
                    return *m_network_link_down_bitmap;
                else
                    return object.get_byte_string("ipv4_address"sv).value_or("").is_empty() ? *m_network_disconnected_bitmap : *m_network_connected_bitmap;
            });
        net_adapters_fields.empend("name", "Name"_string, Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("class_name", "Class"_string, Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("mac_address", "MAC"_string, Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("Link status"_string, Gfx::TextAlignment::CenterLeft,
            [](JsonObject const& object) -> ByteString {
                if (!object.get_bool("link_up"sv).value_or(false))
                    return "Down";

                return ByteString::formatted("{} Mb/s {}-duplex", object.get_i32("link_speed"sv).value_or(0),
                    object.get_bool("link_full_duplex"sv).value_or(false) ? "full"sv : "half"sv);
            });
        net_adapters_fields.empend("IPv4"_string, Gfx::TextAlignment::CenterLeft,
            [](JsonObject const& object) -> ByteString {
                return object.get_byte_string("ipv4_address"sv).value_or(""sv);
            });
        net_adapters_fields.empend("IPv6"_string, Gfx::TextAlignment::CenterLeft,
            [](JsonObject const& object) -> ByteString {
                return object.get_byte_string("ipv6_address"sv).value_or(""sv);
            });
        net_adapters_fields.empend("packets_in", "Pkt In"_string, Gfx::TextAlignment::CenterRight);
        net_adapters_fields.empend("packets_out", "Pkt Out"_string, Gfx::TextAlignment::CenterRight);
        net_adapters_fields.empend("bytes_in", "Bytes In"_string, Gfx::TextAlignment::CenterRight);
        net_adapters_fields.empend("bytes_out", "Bytes Out"_string, Gfx::TextAlignment::CenterRight);
        net_adapters_fields.empend("packets_dropped", "Packets Dropped"_string, Gfx::TextAlignment::CenterRight);
        m_adapter_model = GUI::JsonArrayModel::create("/sys/kernel/net/adapters", move(net_adapters_fields));
        m_adapter_table_view->set_model(MUST(GUI::SortingProxyModel::create(*m_adapter_model)));
        m_adapter_context_menu = GUI::Menu::construct();
        m_adapter_context_menu->add_action(GUI::Action::create(
            "Open in Network Settings...", MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/network.png"sv)), [this](GUI::Action&) {
                m_adapter_table_view->selection().for_each_index([this](GUI::ModelIndex const& index) {
                    auto adapter_name = index.sibling_at_column(1).data().as_string();
                    GUI::Process::spawn_or_show_error(window(), "/bin/NetworkSettings"sv, Array { adapter_name.characters() });
                });
            },
            this));
        m_adapter_table_view->on_context_menu_request = [this](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
            if (!index.is_valid()) {
                return;
            }
            auto adapter_name = index.sibling_at_column(1).data().as_string();
            if (adapter_name == "loop") {
                return;
            }
            m_adapter_context_menu->popup(event.screen_position());
        };

        auto& tcp_sockets_group_box = add<GUI::GroupBox>("TCP Sockets"sv);
        tcp_sockets_group_box.set_layout<GUI::VerticalBoxLayout>(6);

        m_tcp_socket_table_view = tcp_sockets_group_box.add<GUI::TableView>();

        Vector<GUI::JsonArrayModel::FieldSpec> net_tcp_fields;
        net_tcp_fields.empend("peer_address", "Peer"_string, Gfx::TextAlignment::CenterLeft);
        net_tcp_fields.empend("peer_port", "Port"_string, Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("local_address", "Local"_string, Gfx::TextAlignment::CenterLeft);
        net_tcp_fields.empend("local_port", "Port"_string, Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("state", "State"_string, Gfx::TextAlignment::CenterLeft);
        net_tcp_fields.empend("ack_number", "Ack#"_string, Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("sequence_number", "Seq#"_string, Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("packets_in", "Pkt In"_string, Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("packets_out", "Pkt Out"_string, Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("bytes_in", "Bytes In"_string, Gfx::TextAlignment::CenterRight);
        net_tcp_fields.empend("bytes_out", "Bytes Out"_string, Gfx::TextAlignment::CenterRight);
        m_tcp_socket_model = GUI::JsonArrayModel::create("/sys/kernel/net/tcp", move(net_tcp_fields));
        m_tcp_socket_table_view->set_model(MUST(GUI::SortingProxyModel::create(*m_tcp_socket_model)));

        auto& udp_sockets_group_box = add<GUI::GroupBox>("UDP Sockets"sv);
        udp_sockets_group_box.set_layout<GUI::VerticalBoxLayout>(6);

        m_udp_socket_table_view = udp_sockets_group_box.add<GUI::TableView>();

        Vector<GUI::JsonArrayModel::FieldSpec> net_udp_fields;
        net_udp_fields.empend("peer_address", "Peer"_string, Gfx::TextAlignment::CenterLeft);
        net_udp_fields.empend("peer_port", "Port"_string, Gfx::TextAlignment::CenterRight);
        net_udp_fields.empend("local_address", "Local"_string, Gfx::TextAlignment::CenterLeft);
        net_udp_fields.empend("local_port", "Port"_string, Gfx::TextAlignment::CenterRight);
        m_udp_socket_model = GUI::JsonArrayModel::create("/sys/kernel/net/udp", move(net_udp_fields));
        m_udp_socket_table_view->set_model(MUST(GUI::SortingProxyModel::create(*m_udp_socket_model)));

        m_update_timer = add<Core::Timer>(
            1000, [this] {
                update_models();
            });
        m_update_timer->start();

        update_models();
    };
}

void NetworkStatisticsWidget::update_models()
{
    m_adapter_model->update();
    m_tcp_socket_model->update();
    m_udp_socket_model->update();
}

}
