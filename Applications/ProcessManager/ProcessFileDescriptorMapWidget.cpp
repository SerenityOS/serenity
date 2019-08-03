#include "ProcessFileDescriptorMapWidget.h"
#include "ProcessFileDescriptorMapModel.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GTableView.h>

ProcessFileDescriptorMapWidget::ProcessFileDescriptorMapWidget(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 4, 4, 4, 4 });
    m_table_view = new GTableView(this);
    m_table_view->set_model(adopt(*new ProcessFileDescriptorMapModel));
}

ProcessFileDescriptorMapWidget::~ProcessFileDescriptorMapWidget()
{
}

void ProcessFileDescriptorMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    static_cast<ProcessFileDescriptorMapModel*>(m_table_view->model())->set_pid(pid);
}
