#include "ProcessMemoryMapWidget.h"
#include <LibCore/CTimer.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GJsonArrayModel.h>
#include <LibGUI/GSortingProxyModel.h>
#include <LibGUI/GTableView.h>

ProcessMemoryMapWidget::ProcessMemoryMapWidget(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 4, 4, 4, 4 });
    m_table_view = GTableView::construct(this);
    m_table_view->set_size_columns_to_fit_content(true);
    Vector<GJsonArrayModel::FieldSpec> pid_vm_fields;
    pid_vm_fields.empend("Address", TextAlignment::CenterLeft, [](auto& object) {
        return String::format("%#x", object.get("address").to_u32());
    });
    pid_vm_fields.empend("size", "Size", TextAlignment::CenterRight);
    pid_vm_fields.empend("amount_resident", "Resident", TextAlignment::CenterRight);
    pid_vm_fields.empend("amount_dirty", "Dirty", TextAlignment::CenterRight);
    pid_vm_fields.empend("Access", TextAlignment::CenterLeft, [](auto& object) {
        StringBuilder builder;
        if (!object.get("user_accessible").to_bool())
            builder.append('K');
        if (object.get("readable").to_bool())
            builder.append('R');
        if (object.get("writable").to_bool())
            builder.append('W');
        if (object.get("executable").to_bool())
            builder.append('X');
        if (object.get("shared").to_bool())
            builder.append('S');
        if (object.get("stack").to_bool())
            builder.append('T');
        return builder.to_string();
    });
    pid_vm_fields.empend("Purgeable", TextAlignment::CenterLeft, [](auto& object) {
        if (!object.get("purgeable").to_bool())
            return "";
        if (object.get("volatile").to_bool())
            return "Volatile";
        return "Non-volatile";
    });
    pid_vm_fields.empend("cow_pages", "# CoW", TextAlignment::CenterRight);
    pid_vm_fields.empend("name", "Name", TextAlignment::CenterLeft);
    m_json_model = GJsonArrayModel::create({}, move(pid_vm_fields));
    m_table_view->set_model(GSortingProxyModel::create(*m_json_model));
    m_table_view->model()->set_key_column_and_sort_order(0, GSortOrder::Ascending);
    m_timer = CTimer::construct(1000, [this] { refresh(); }, this);
}

ProcessMemoryMapWidget::~ProcessMemoryMapWidget()
{
}

void ProcessMemoryMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    m_json_model->set_json_path(String::format("/proc/%d/vm", pid));
}

void ProcessMemoryMapWidget::refresh()
{
    if (m_pid != -1)
        m_json_model->update();
}
