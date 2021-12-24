/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessFileDescriptorMapWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>

ProcessFileDescriptorMapWidget::ProcessFileDescriptorMapWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins(4);
    m_table_view = add<GUI::TableView>();

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

    m_model = GUI::JsonArrayModel::create({}, move(pid_fds_fields));
    m_table_view->set_model(MUST(GUI::SortingProxyModel::create(*m_model)));
}

ProcessFileDescriptorMapWidget::~ProcessFileDescriptorMapWidget()
{
}

void ProcessFileDescriptorMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    m_model->set_json_path(String::formatted("/proc/{}/fds", m_pid));
}
