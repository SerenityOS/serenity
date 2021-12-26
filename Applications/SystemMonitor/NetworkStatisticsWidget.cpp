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
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GJsonArrayModel.h>
#include <LibGUI/GTableView.h>

NetworkStatisticsWidget::NetworkStatisticsWidget(GWidget* parent)
    : GLazyWidget(parent)
{
    on_first_show = [this](auto&) {
        set_layout(make<GVBoxLayout>());
        layout()->set_margins({ 4, 4, 4, 4 });
        set_fill_with_background_color(true);

        auto adapters_group_box = GGroupBox::construct("Adapters", this);
        adapters_group_box->set_layout(make<GVBoxLayout>());
        adapters_group_box->layout()->set_margins({ 6, 16, 6, 6 });
        adapters_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        adapters_group_box->set_preferred_size(0, 120);

        m_adapter_table_view = GTableView::construct(adapters_group_box);
        m_adapter_table_view->set_size_columns_to_fit_content(true);

        Vector<GJsonArrayModel::FieldSpec> net_adapters_fields;
        net_adapters_fields.empend("name", "Name", TextAlignment::CenterLeft);
        net_adapters_fields.empend("class_name", "Class", TextAlignment::CenterLeft);
        net_adapters_fields.empend("mac_address", "MAC", TextAlignment::CenterLeft);
        net_adapters_fields.empend("ipv4_address", "IPv4", TextAlignment::CenterLeft);
        net_adapters_fields.empend("packets_in", "Pkt In", TextAlignment::CenterRight);
        net_adapters_fields.empend("packets_out", "Pkt Out", TextAlignment::CenterRight);
        net_adapters_fields.empend("bytes_in", "Bytes In", TextAlignment::CenterRight);
        net_adapters_fields.empend("bytes_out", "Bytes Out", TextAlignment::CenterRight);
        m_adapter_table_view->set_model(GJsonArrayModel::create("/proc/net/adapters", move(net_adapters_fields)));

        auto sockets_group_box = GGroupBox::construct("Sockets", this);
        sockets_group_box->set_layout(make<GVBoxLayout>());
        sockets_group_box->layout()->set_margins({ 6, 16, 6, 6 });
        sockets_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
        sockets_group_box->set_preferred_size(0, 0);

        m_socket_table_view = GTableView::construct(sockets_group_box);
        m_socket_table_view->set_size_columns_to_fit_content(true);

        Vector<GJsonArrayModel::FieldSpec> net_tcp_fields;
        net_tcp_fields.empend("peer_address", "Peer", TextAlignment::CenterLeft);
        net_tcp_fields.empend("peer_port", "Port", TextAlignment::CenterRight);
        net_tcp_fields.empend("local_address", "Local", TextAlignment::CenterLeft);
        net_tcp_fields.empend("local_port", "Port", TextAlignment::CenterRight);
        net_tcp_fields.empend("state", "State", TextAlignment::CenterLeft);
        net_tcp_fields.empend("ack_number", "Ack#", TextAlignment::CenterRight);
        net_tcp_fields.empend("sequence_number", "Seq#", TextAlignment::CenterRight);
        net_tcp_fields.empend("packets_in", "Pkt In", TextAlignment::CenterRight);
        net_tcp_fields.empend("packets_out", "Pkt Out", TextAlignment::CenterRight);
        net_tcp_fields.empend("bytes_in", "Bytes In", TextAlignment::CenterRight);
        net_tcp_fields.empend("bytes_out", "Bytes Out", TextAlignment::CenterRight);
        m_socket_table_view->set_model(GJsonArrayModel::create("/proc/net/tcp", move(net_tcp_fields)));

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
