/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ProcessStatisticsReader.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/RunningProcessesModel.h>

namespace GUI {

NonnullRefPtr<RunningProcessesModel> RunningProcessesModel::create()
{
    return adopt_ref(*new RunningProcessesModel);
}

void RunningProcessesModel::update()
{
    m_processes.clear();

    auto all_processes = Core::ProcessStatisticsReader::get_all();
    if (!all_processes.is_error()) {
        for (auto& it : all_processes.value().processes) {
            Process process;
            process.pid = it.pid;
            process.uid = it.uid;
            process.icon = FileIconProvider::icon_for_executable(it.executable).bitmap_for_size(16);
            process.name = it.name;
            m_processes.append(move(process));
        }
    }

    did_update();
}

int RunningProcessesModel::row_count(const GUI::ModelIndex&) const
{
    return m_processes.size();
}

int RunningProcessesModel::column_count(const GUI::ModelIndex&) const
{
    return Column::__Count;
}

ErrorOr<String> RunningProcessesModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::Icon:
        return String {};
    case Column::PID:
        return "PID"_string;
    case Column::UID:
        return "UID"_string;
    case Column::Name:
        return "Name"_string;
    }
    VERIFY_NOT_REACHED();
}

GUI::Variant RunningProcessesModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto& process = m_processes[index.row()];

    if (role == ModelRole::Custom) {
        return process.pid;
    }

    if (role == ModelRole::Display) {
        switch (index.column()) {
        case Column::Icon:
            if (!process.icon)
                return GUI::Icon();
            return GUI::Icon(*process.icon);
        case Column::PID:
            return process.pid;
        case Column::UID:
            return process.uid;
        case Column::Name:
            return process.name;
        }
        VERIFY_NOT_REACHED();
    }
    return {};
}

}
