#include "ProcessTableView.h"
#include "ProcessModel.h"
#include <LibGUI/GSortingProxyModel.h>
#include <stdio.h>

ProcessTableView::ProcessTableView(GraphWidget& graph, GWidget* parent)
    : GTableView(parent)
{
    set_model(GSortingProxyModel::create(ProcessModel::create(graph)));
    model()->set_key_column_and_sort_order(ProcessModel::Column::CPU, GSortOrder::Descending);
    refresh();

    on_selection = [this](auto&) {
        if (on_process_selected)
            on_process_selected(selected_pid());
    };
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
