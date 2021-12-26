#include "NetworkStatisticsWidget.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GJsonArrayModel.h>
#include <LibGUI/GTableView.h>

NetworkStatisticsWidget::NetworkStatisticsWidget(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 4, 4, 4, 4 });
    set_fill_with_background_color(true);
    set_background_color(Color::WarmGray);

    auto* adapters_group_box = new GGroupBox("Adapters", this);
    adapters_group_box->set_layout(make<GBoxLayout>(Orientation::Vertical));
    adapters_group_box->layout()->set_margins({ 6, 16, 6, 6 });
    adapters_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    adapters_group_box->set_preferred_size(0, 120);

    m_adapter_table_view = new GTableView(adapters_group_box);
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

    auto* sockets_group_box = new GGroupBox("Sockets", this);
    sockets_group_box->set_layout(make<GBoxLayout>(Orientation::Vertical));
    sockets_group_box->layout()->set_margins({ 6, 16, 6, 6 });
    sockets_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
    sockets_group_box->set_preferred_size(0, 0);

    m_socket_table_view = new GTableView(sockets_group_box);
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

    m_update_timer = CTimer::create(
        1000, [this] {
            update_models();
        },
        this);

    update_models();
}

NetworkStatisticsWidget::~NetworkStatisticsWidget()
{
}

void NetworkStatisticsWidget::update_models()
{
    m_adapter_table_view->model()->update();
    m_socket_table_view->model()->update();
}
