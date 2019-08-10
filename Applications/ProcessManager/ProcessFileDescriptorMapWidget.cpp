#include "ProcessFileDescriptorMapWidget.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GJsonArrayModel.h>
#include <LibGUI/GTableView.h>

ProcessFileDescriptorMapWidget::ProcessFileDescriptorMapWidget(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 4, 4, 4, 4 });
    m_table_view = new GTableView(this);
    m_table_view->set_size_columns_to_fit_content(true);

    Vector<GJsonArrayModel::FieldSpec> pid_fds_fields;
    pid_fds_fields.empend("fd", "FD", TextAlignment::CenterRight);
    pid_fds_fields.empend("class", "Class", TextAlignment::CenterLeft);
    pid_fds_fields.empend("offset", "Offset", TextAlignment::CenterRight);
    pid_fds_fields.empend("absolute_path", "Path", TextAlignment::CenterLeft);
    pid_fds_fields.empend("seekable", "Access", TextAlignment::CenterLeft, [](auto& data) {
        return data.to_bool() ? "Seekable" : "Sequential";
    });

    m_table_view->set_model(GJsonArrayModel::create({}, move(pid_fds_fields)));
}

ProcessFileDescriptorMapWidget::~ProcessFileDescriptorMapWidget()
{
}

void ProcessFileDescriptorMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    static_cast<GJsonArrayModel*>(m_table_view->model())->set_json_path(String::format("/proc/%d/fds", m_pid));
}
