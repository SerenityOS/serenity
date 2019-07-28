#include "ProcessMemoryMapWidget.h"
#include "ProcessMemoryMapModel.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GTableView.h>

ProcessMemoryMapWidget::ProcessMemoryMapWidget(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 4, 4, 4, 4 });
    m_table_view = new GTableView(this);
    m_table_view->set_model(adopt(*new ProcessMemoryMapModel));
}

ProcessMemoryMapWidget::~ProcessMemoryMapWidget()
{
}

void ProcessMemoryMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    static_cast<ProcessMemoryMapModel*>(m_table_view->model())->set_pid(pid);
}
