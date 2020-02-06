/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "NetworkStatisticsWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/TableView.h>

NetworkStatisticsWidget::NetworkStatisticsWidget(GUI::Widget* parent)
    : GUI::LazyWidget(parent)
{
    on_first_show = [this](auto&) {
        set_layout(make<GUI::VerticalBoxLayout>());
        layout()->set_margins({ 4, 4, 4, 4 });
        set_fill_with_background_color(true);

        auto adapters_group_box = GUI::GroupBox::construct("Adapters", this);
        adapters_group_box->set_layout(make<GUI::VerticalBoxLayout>());
        adapters_group_box->layout()->set_margins({ 6, 16, 6, 6 });
        adapters_group_box->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        adapters_group_box->set_preferred_size(0, 120);

        m_adapter_table_view = GUI::TableView::construct(adapters_group_box);
        m_adapter_table_view->set_size_columns_to_fit_content(true);

        Vector<GUI::JsonArrayModel::FieldSpec> net_adapters_fields;
        net_adapters_fields.empend("name", "Name", Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("class_name", "Class", Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("mac_address", "MAC", Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("ipv4_address", "IPv4", Gfx::TextAlignment::CenterLeft);
        net_adapters_fields.empend("packets_in", "Pkt In", Gfx::TextAlignment::CenterRight);
        net_adapters_fields.empend("packets_out", "Pkt Out", Gfx::TextAlignment::CenterRight);
        net_adapters_fields.empend("bytes_in", "Bytes In", Gfx::TextAlignment::CenterRight);
        net_adapters_fields.empend("bytes_out", "Bytes Out", Gfx::TextAlignment::CenterRight);
        m_adapter_table_view->set_model(GUI::JsonArrayModel::create("/proc/net/adapters", move(net_adapters_fields)));

        auto sockets_group_box = GUI::GroupBox::construct("Sockets", this);
        sockets_group_box->set_layout(make<GUI::VerticalBoxLayout>());
        sockets_group_box->layout()->set_margins({ 6, 16, 6, 6 });
        sockets_group_box->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
        sockets_group_box->set_preferred_size(0, 0);

        m_socket_table_view = GUI::TableView::construct(sockets_group_box);
        m_socket_table_view->set_size_columns_to_fit_content(true);

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
        m_socket_table_view->set_model(GUI::JsonArrayModel::create("/proc/net/tcp", move(net_tcp_fields)));

        m_update_timer = Core::Timer::construct(
            1000, [this] {
                update_models();
            },
            this);

        update_models();
    };
}

NetworkStatisticsWidget::~NetworkStatisticsWidget()
{
}

void NetworkStatisticsWidget::update_models()
{
    m_adapter_table_view->model()->update();
    m_socket_table_view->model()->update();
}
