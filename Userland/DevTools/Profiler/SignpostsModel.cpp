/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

SignpostsModel::~SignpostsModel()
{
}

int SignpostsModel::row_count(const GUI::ModelIndex&) const
{
    return m_profile.filtered_signpost_indices().size();
}

int SignpostsModel::column_count(const GUI::ModelIndex&) const
{
    return Column::__Count;
}

String SignpostsModel::column_name(int column) const
{
    switch (column) {
    case Column::SignpostIndex:
        return "#";
    case Column::Timestamp:
        return "Timestamp";
    case Column::ProcessID:
        return "PID";
    case Column::ThreadID:
        return "TID";
    case Column::ExecutableName:
        return "Executable";
    case Column::SignpostString:
        return "String";
    case Column::SignpostArgument:
        return "Argument";
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant SignpostsModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    u32 event_index = m_profile.filtered_signpost_indices()[index.row()];
    auto& event = m_profile.events().at(event_index);

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
            if (auto* process = m_profile.find_process(event.pid, event.serial))
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
