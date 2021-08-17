/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessUnveiledPathsWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>

ProcessUnveiledPathsWidget::ProcessUnveiledPathsWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins(4);
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
