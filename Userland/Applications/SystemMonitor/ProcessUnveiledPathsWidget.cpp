/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessUnveiledPathsWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Widget.h>

REGISTER_WIDGET(SystemMonitor, ProcessUnveiledPathsWidget)

namespace SystemMonitor {

ErrorOr<NonnullRefPtr<ProcessUnveiledPathsWidget>> ProcessUnveiledPathsWidget::try_create()
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ProcessUnveiledPathsWidget()));
    widget->set_layout<GUI::VerticalBoxLayout>(4);
    widget->m_table_view = widget->add<GUI::TableView>();

    Vector<GUI::JsonArrayModel::FieldSpec> pid_unveil_fields;
    TRY(pid_unveil_fields.try_empend("path", "Path"_string, Gfx::TextAlignment::CenterLeft));
    TRY(pid_unveil_fields.try_empend("permissions", "Permissions"_string, Gfx::TextAlignment::CenterLeft));

    widget->m_model = GUI::JsonArrayModel::create({}, move(pid_unveil_fields));
    widget->m_table_view->set_model(TRY(GUI::SortingProxyModel::create(*widget->m_model)));
    return widget;
}

void ProcessUnveiledPathsWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    m_model->set_json_path(ByteString::formatted("/proc/{}/unveil", m_pid));
}

}
