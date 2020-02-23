/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "ProcessMemoryMapWidget.h"
#include <LibCore/Timer.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>

ProcessMemoryMapWidget::ProcessMemoryMapWidget(GUI::Widget* parent)
    : GUI::Widget(parent)
{
    set_layout(make<GUI::VerticalBoxLayout>());
    layout()->set_margins({ 4, 4, 4, 4 });
    m_table_view = add<GUI::TableView>();
    m_table_view->set_size_columns_to_fit_content(true);
    Vector<GUI::JsonArrayModel::FieldSpec> pid_vm_fields;
    pid_vm_fields.empend("Address", Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return String::format("%#x", object.get("address").to_u32());
    });
    pid_vm_fields.empend("size", "Size", Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("amount_resident", "Resident", Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("amount_dirty", "Dirty", Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("Access", Gfx::TextAlignment::CenterLeft, [](auto& object) {
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
    pid_vm_fields.empend("Purgeable", Gfx::TextAlignment::CenterLeft, [](auto& object) {
        if (!object.get("purgeable").to_bool())
            return "";
        if (object.get("volatile").to_bool())
            return "Volatile";
        return "Non-volatile";
    });
    pid_vm_fields.empend("cow_pages", "# CoW", Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("name", "Name", Gfx::TextAlignment::CenterLeft);
    m_json_model = GUI::JsonArrayModel::create({}, move(pid_vm_fields));
    m_table_view->set_model(GUI::SortingProxyModel::create(*m_json_model));
    m_table_view->model()->set_key_column_and_sort_order(0, GUI::SortOrder::Ascending);
    m_timer = add<Core::Timer>(1000, [this] { refresh(); });
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
