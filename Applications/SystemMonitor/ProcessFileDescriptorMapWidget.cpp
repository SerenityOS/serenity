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

#include "ProcessFileDescriptorMapWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/TableView.h>

ProcessFileDescriptorMapWidget::ProcessFileDescriptorMapWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins({ 4, 4, 4, 4 });
    m_table_view = add<GUI::TableView>();
    m_table_view->set_size_columns_to_fit_content(true);

    Vector<GUI::JsonArrayModel::FieldSpec> pid_fds_fields;
    pid_fds_fields.empend("fd", "FD", Gfx::TextAlignment::CenterRight);
    pid_fds_fields.empend("class", "Class", Gfx::TextAlignment::CenterLeft);
    pid_fds_fields.empend("offset", "Offset", Gfx::TextAlignment::CenterRight);
    pid_fds_fields.empend("absolute_path", "Path", Gfx::TextAlignment::CenterLeft);
    pid_fds_fields.empend("Access", Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get("seekable").to_bool() ? "Seekable" : "Sequential";
    });
    pid_fds_fields.empend("Blocking", Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get("blocking").to_bool() ? "Blocking" : "Nonblocking";
    });
    pid_fds_fields.empend("On exec", Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get("cloexec").to_bool() ? "Close" : "Keep";
    });
    pid_fds_fields.empend("Can read", Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get("can_read").to_bool() ? "Yes" : "No";
    });
    pid_fds_fields.empend("Can write", Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get("can_write").to_bool() ? "Yes" : "No";
    });

    m_table_view->set_model(GUI::JsonArrayModel::create({}, move(pid_fds_fields)));
}

ProcessFileDescriptorMapWidget::~ProcessFileDescriptorMapWidget()
{
}

void ProcessFileDescriptorMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    static_cast<GUI::JsonArrayModel*>(m_table_view->model())->set_json_path(String::format("/proc/%d/fds", m_pid));
}
