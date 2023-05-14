/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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
    m_user_frame_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"sv).release_value_but_fixme_should_propagate_errors());
    m_kernel_frame_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object-red.png"sv).release_value_but_fixme_should_propagate_errors());
}

int SamplesModel::row_count(GUI::ModelIndex const&) const
{
    return m_profile.filtered_event_indices().size();
}

int SamplesModel::column_count(GUI::ModelIndex const&) const
{
    return Column::__Count;
}

String SamplesModel::column_name(int column) const
{
    switch (column) {
    case Column::SampleIndex:
        return "#"_short_string;
    case Column::Timestamp:
        return "Timestamp"_string.release_value_but_fixme_should_propagate_errors();
    case Column::ProcessID:
        return "PID"_short_string;
    case Column::ThreadID:
        return "TID"_short_string;
    case Column::ExecutableName:
        return "Executable"_string.release_value_but_fixme_should_propagate_errors();
    case Column::LostSamples:
        return "Lost Samples"_string.release_value_but_fixme_should_propagate_errors();
    case Column::InnermostStackFrame:
        return "Innermost Frame"_string.release_value_but_fixme_should_propagate_errors();
    case Column::Path:
        return "Path"_short_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant SamplesModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    u32 event_index = m_profile.filtered_event_indices()[index.row()];
    auto const& event = m_profile.events().at(event_index);

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
            if (auto const* process = m_profile.find_process(event.pid, event.serial))
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

        if (index.column() == Column::Path) {
            if (!event.data.has<Profile::Event::ReadData>())
                return "";
            return event.data.get<Profile::Event::ReadData>().path;
        }

        return {};
    }
    return {};
}

}
