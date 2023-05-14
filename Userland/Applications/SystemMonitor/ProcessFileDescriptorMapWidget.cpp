/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessFileDescriptorMapWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>

REGISTER_WIDGET(SystemMonitor, ProcessFileDescriptorMapWidget)

namespace SystemMonitor {

ProcessFileDescriptorMapWidget::ProcessFileDescriptorMapWidget()
{
    set_layout<GUI::VerticalBoxLayout>(4);
    m_table_view = add<GUI::TableView>();

    Vector<GUI::JsonArrayModel::FieldSpec> pid_fds_fields;
    pid_fds_fields.empend("fd", "FD"_short_string, Gfx::TextAlignment::CenterRight);
    pid_fds_fields.empend("class", "Class"_short_string, Gfx::TextAlignment::CenterLeft);
    pid_fds_fields.empend("offset", "Offset"_short_string, Gfx::TextAlignment::CenterRight);
    pid_fds_fields.empend("absolute_path", "Path"_short_string, Gfx::TextAlignment::CenterLeft);
    pid_fds_fields.empend("Access"_short_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get_bool("seekable"sv).value_or(false) ? "Seekable" : "Sequential";
    });
    pid_fds_fields.empend("Blocking"_string.release_value_but_fixme_should_propagate_errors(), Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get_bool("blocking"sv).value_or(false) ? "Blocking" : "Nonblocking";
    });
    pid_fds_fields.empend("On exec"_short_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get_bool("cloexec"sv).value_or(false) ? "Close" : "Keep";
    });
    pid_fds_fields.empend("Can read"_string.release_value_but_fixme_should_propagate_errors(), Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get_bool("can_read"sv).value_or(false) ? "Yes" : "No";
    });
    pid_fds_fields.empend("Can write"_string.release_value_but_fixme_should_propagate_errors(), Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get_bool("can_write"sv).value_or(false) ? "Yes" : "No";
    });

    m_model = GUI::JsonArrayModel::create({}, move(pid_fds_fields));
    m_table_view->set_model(MUST(GUI::SortingProxyModel::create(*m_model)));
}

void ProcessFileDescriptorMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    m_model->set_json_path(DeprecatedString::formatted("/proc/{}/fds", m_pid));
}

}
