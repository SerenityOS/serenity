#include "ProcessTableView.h"
#include "ProcessTableModel.h"


ProcessTableView::ProcessTableView(GWidget* parent)
    : GTableView(parent)
{
    set_model(make<ProcessTableModel>());
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

pid_t ProcessTableView::selected_pid() const
{
    return model().selected_pid();
}

inline ProcessTableModel& ProcessTableView::model()
{
    return static_cast<ProcessTableModel&>(*GTableView::model());
}

inline const ProcessTableModel& ProcessTableView::model() const
{
    return static_cast<const ProcessTableModel&>(*GTableView::model());
}
