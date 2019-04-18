#include "ProcessTableView.h"
#include "ProcessModel.h"
#include <LibGUI/GSortingProxyModel.h>
#include <stdio.h>

ProcessTableView::ProcessTableView(GWidget* parent)
    : GTableView(parent)
{
    set_model(GSortingProxyModel::create(ProcessModel::create()));
    model()->set_key_column_and_sort_order(ProcessModel::Column::CPU, GSortOrder::Descending);
    refresh();
}

ProcessTableView::~ProcessTableView()
{
}

void ProcessTableView::refresh()
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
    return model()->data(model()->index(model()->selected_index().row(), ProcessModel::Column::PID), GModel::Role::Sort).as_int();
}
