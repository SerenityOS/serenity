#include "ProcessTableView.h"
#include "ProcessTableModel.h"
#include <LibGUI/GSortingProxyTableModel.h>
#include <stdio.h>

ProcessTableView::ProcessTableView(GWidget* parent)
    : GTableView(parent)
{
    auto process_model = make<ProcessTableModel>();
    m_model = process_model.ptr();
    set_model(make<GSortingProxyTableModel>(move(process_model)));
    GTableView::model()->set_key_column_and_sort_order(ProcessTableModel::Column::CPU, GSortOrder::Descending);
    start_timer(1000);
    model().update();
}

ProcessTableView::~ProcessTableView()
{
}

void ProcessTableView::timer_event(GTimerEvent&)
{
    model().update();
}

void ProcessTableView::model_notification(const GModelNotification& notification)
{
    if (notification.type() == GModelNotification::ModelUpdated) {
        if (on_status_message)
            on_status_message(String::format("%d processes", model().row_count()));
        return;
    }
}

pid_t ProcessTableView::selected_pid() const
{
    return model().selected_pid();
}
