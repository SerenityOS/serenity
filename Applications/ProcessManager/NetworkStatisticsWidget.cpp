#include "NetworkStatisticsWidget.h"
#include "NetworkAdapterModel.h"
#include "SocketModel.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GGroupBox.h>
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
    m_adapter_table_view->set_model(NetworkAdapterModel::create());

    auto* sockets_group_box = new GGroupBox("Sockets", this);
    sockets_group_box->set_layout(make<GBoxLayout>(Orientation::Vertical));
    sockets_group_box->layout()->set_margins({ 6, 16, 6, 6 });
    sockets_group_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
    sockets_group_box->set_preferred_size(0, 0);

    m_socket_table_view = new GTableView(sockets_group_box);
    m_socket_table_view->set_size_columns_to_fit_content(true);
    m_socket_table_view->set_model(SocketModel::create());

    m_update_timer = new CTimer(
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
