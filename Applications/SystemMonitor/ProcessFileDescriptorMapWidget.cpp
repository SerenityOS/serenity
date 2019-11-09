#include "ProcessFileDescriptorMapWidget.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GJsonArrayModel.h>
#include <LibGUI/GTableView.h>

ProcessFileDescriptorMapWidget::ProcessFileDescriptorMapWidget(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 4, 4, 4, 4 });
    m_table_view = GTableView::construct(this);
    m_table_view->set_size_columns_to_fit_content(true);

    Vector<GJsonArrayModel::FieldSpec> pid_fds_fields;
    pid_fds_fields.empend("fd", "FD", TextAlignment::CenterRight);
    pid_fds_fields.empend("class", "Class", TextAlignment::CenterLeft);
    pid_fds_fields.empend("offset", "Offset", TextAlignment::CenterRight);
    pid_fds_fields.empend("absolute_path", "Path", TextAlignment::CenterLeft);
    pid_fds_fields.empend("Access", TextAlignment::CenterLeft, [](auto& object) {
        return object.get("seekable").to_bool() ? "Seekable" : "Sequential";
    });
    pid_fds_fields.empend("Blocking", TextAlignment::CenterLeft, [](auto& object) {
        return object.get("blocking").to_bool() ? "Blocking" : "Nonblocking";
    });
    pid_fds_fields.empend("On exec", TextAlignment::CenterLeft, [](auto& object) {
        return object.get("cloexec").to_bool() ? "Close" : "Keep";
    });
    pid_fds_fields.empend("Can read", TextAlignment::CenterLeft, [](auto& object) {
        return object.get("can_read").to_bool() ? "Yes" : "No";
    });
    pid_fds_fields.empend("Can write", TextAlignment::CenterLeft, [](auto& object) {
        return object.get("can_write").to_bool() ? "Yes" : "No";
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
