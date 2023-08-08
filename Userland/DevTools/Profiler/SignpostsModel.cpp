/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SignpostsModel.h"
#include "Profile.h"
#include <AK/StringBuilder.h>

namespace Profiler {

SignpostsModel::SignpostsModel(Profile& profile)
    : m_profile(profile)
{
}

int SignpostsModel::row_count(GUI::ModelIndex const&) const
{
    return m_profile.filtered_signpost_indices().size();
}

int SignpostsModel::column_count(GUI::ModelIndex const&) const
{
    return Column::__Count;
}

ErrorOr<String> SignpostsModel::column_name(int column) const
{
    switch (column) {
    case Column::SignpostIndex:
        return "#"_string;
    case Column::Timestamp:
        return "Timestamp"_string;
    case Column::ProcessID:
        return "PID"_string;
    case Column::ThreadID:
        return "TID"_string;
    case Column::ExecutableName:
        return "Executable"_string;
    case Column::SignpostString:
        return "String"_string;
    case Column::SignpostArgument:
        return "Argument"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant SignpostsModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    u32 event_index = m_profile.filtered_signpost_indices()[index.row()];
    auto const& event = m_profile.events().at(event_index);

    if (role == GUI::ModelRole::Custom) {
        return event_index;
    }

    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::SignpostIndex)
            return event_index;

        if (index.column() == Column::ProcessID)
            return event.pid;

        if (index.column() == Column::ThreadID)
            return event.tid;

        if (index.column() == Column::ExecutableName) {
            if (auto const* process = m_profile.find_process(event.pid, event.serial))
                return process->executable;
            return "";
        }

        if (index.column() == Column::Timestamp) {
            return (u32)event.timestamp;
        }

        if (index.column() == Column::SignpostString) {
            return event.data.get<Profile::Event::SignpostData>().string;
        }

        if (index.column() == Column::SignpostArgument) {
            return event.data.get<Profile::Event::SignpostData>().arg;
        }
        return {};
    }
    return {};
}

}
