#include "ProcessTableView.h"
#include "ProcessTableModel.h"
#include <LibGUI/GSortingProxyTableModel.h>
#include <stdio.h>

ProcessTableView::ProcessTableView(GWidget* parent)
    : GTableView(parent)
{
    set_model(make<GSortingProxyTableModel>(make<ProcessTableModel>()));
    model()->set_key_column_and_sort_order(ProcessTableModel::Column::CPU, GSortOrder::Descending);
    start_timer(1000);
    model()->update();
}

ProcessTableView::~ProcessTableView()
{
}

void ProcessTableView::timer_event(GTimerEvent&)
{
    model()->update();
}

void ProcessTableView::model_notification(const GModelNotification& notification)
{
    if (notification.type() == GModelNotification::ModelUpdated) {
        // Do something?
        return;
    }
}

pid_t ProcessTableView::selected_pid() const
{
    if (!model()->selected_index().is_valid())
        return -1;
    return model()->data({ model()->selected_index().row(), ProcessTableModel::Column::PID }, GTableModel::Role::Sort).as_int();
}
