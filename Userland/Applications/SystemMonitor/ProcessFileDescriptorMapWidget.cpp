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

ErrorOr<NonnullRefPtr<ProcessFileDescriptorMapWidget>> ProcessFileDescriptorMapWidget::try_create()
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ProcessFileDescriptorMapWidget()));
    widget->set_layout<GUI::VerticalBoxLayout>(4);
    widget->m_table_view = widget->add<GUI::TableView>();

    Vector<GUI::JsonArrayModel::FieldSpec> pid_fds_fields;
    TRY(pid_fds_fields.try_empend("fd", "FD"_string, Gfx::TextAlignment::CenterRight));
    TRY(pid_fds_fields.try_empend("class", "Class"_string, Gfx::TextAlignment::CenterLeft));
    TRY(pid_fds_fields.try_empend("offset", "Offset"_string, Gfx::TextAlignment::CenterRight));
    TRY(pid_fds_fields.try_empend("absolute_path", "Path"_string, Gfx::TextAlignment::CenterLeft));
    TRY(pid_fds_fields.try_empend("Access"_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get_bool("seekable"sv).value_or(false) ? "Seekable" : "Sequential";
    }));
    TRY(pid_fds_fields.try_empend("Blocking"_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get_bool("blocking"sv).value_or(false) ? "Blocking" : "Nonblocking";
    }));
    TRY(pid_fds_fields.try_empend("On exec"_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get_bool("cloexec"sv).value_or(false) ? "Close" : "Keep";
    }));
    TRY(pid_fds_fields.try_empend("Can read"_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get_bool("can_read"sv).value_or(false) ? "Yes" : "No";
    }));
    TRY(pid_fds_fields.try_empend("Can write"_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
        return object.get_bool("can_write"sv).value_or(false) ? "Yes" : "No";
    }));

    widget->m_model = GUI::JsonArrayModel::create({}, move(pid_fds_fields));
    widget->m_table_view->set_model(TRY(GUI::SortingProxyModel::create(*widget->m_model)));

    return widget;
}

void ProcessFileDescriptorMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    m_model->set_json_path(ByteString::formatted("/proc/{}/fds", m_pid));
}

}
