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

#include "ProcessUnveiledPathsWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>

ProcessUnveiledPathsWidget::ProcessUnveiledPathsWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins({ 4, 4, 4, 4 });
    m_table_view = add<GUI::TableView>();

    Vector<GUI::JsonArrayModel::FieldSpec> pid_unveil_fields;
    pid_unveil_fields.empend("path", "Path", Gfx::TextAlignment::CenterLeft);
    pid_unveil_fields.empend("permissions", "Permissions", Gfx::TextAlignment::CenterLeft);

    m_model = GUI::JsonArrayModel::create({}, move(pid_unveil_fields));
    m_table_view->set_model(GUI::SortingProxyModel::create(*m_model));
}

ProcessUnveiledPathsWidget::~ProcessUnveiledPathsWidget()
{
}

void ProcessUnveiledPathsWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    m_model->set_json_path(String::formatted("/proc/{}/unveil", m_pid));
}
