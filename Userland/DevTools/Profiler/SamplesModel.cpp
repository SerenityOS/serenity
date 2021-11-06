/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SamplesModel.h"
#include "Profile.h"
#include <AK/StringBuilder.h>

namespace Profiler {

SamplesModel::SamplesModel(Profile& profile)
    : m_profile(profile)
{
    m_user_frame_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/inspector-object.png").release_value_but_fixme_should_propagate_errors());
    m_kernel_frame_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/inspector-object-red.png").release_value_but_fixme_should_propagate_errors());
}

SamplesModel::~SamplesModel()
{
}

int SamplesModel::row_count(const GUI::ModelIndex&) const
{
    return m_profile.filtered_event_indices().size();
}

int SamplesModel::column_count(const GUI::ModelIndex&) const
{
    return Column::__Count;
}

String SamplesModel::column_name(int column) const
{
    switch (column) {
    case Column::SampleIndex:
        return "#";
    case Column::Timestamp:
        return "Timestamp";
    case Column::ProcessID:
        return "PID";
    case Column::ThreadID:
        return "TID";
    case Column::ExecutableName:
        return "Executable";
    case Column::LostSamples:
        return "Lost Samples";
    case Column::InnermostStackFrame:
        return "Innermost Frame";
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant SamplesModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    u32 event_index = m_profile.filtered_event_indices()[index.row()];
    auto& event = m_profile.events().at(event_index);

    if (role == GUI::ModelRole::Custom) {
        return event_index;
    }

    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::SampleIndex)
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

        if (index.column() == Column::LostSamples) {
            return event.lost_samples;
        }

        if (index.column() == Column::InnermostStackFrame) {
            return event.frames.last().symbol;
        }
        return {};
    }
    return {};
}

}
