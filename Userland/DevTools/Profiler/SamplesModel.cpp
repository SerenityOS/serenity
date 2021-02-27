/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "SamplesModel.h"
#include "Profile.h"
#include <AK/StringBuilder.h>
#include <stdio.h>

SamplesModel::SamplesModel(Profile& profile)
    : m_profile(profile)
{
    m_user_frame_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"));
    m_kernel_frame_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object-red.png"));
}

SamplesModel::~SamplesModel()
{
}

int SamplesModel::row_count(const GUI::ModelIndex&) const
{
    return m_profile.filtered_event_count();
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
    case Column::InnermostStackFrame:
        return "Innermost Frame";
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant SamplesModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    u32 event_index = m_profile.first_filtered_event_index() + index.row();
    auto& event = m_profile.events().at(event_index);

    if (role == GUI::ModelRole::Custom) {
        return event_index;
    }

    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::SampleIndex)
            return event_index;

        if (index.column() == Column::Timestamp) {
            return (u32)event.timestamp;
        }

        if (index.column() == Column::InnermostStackFrame) {
            return event.frames.last().symbol;
        }
        return {};
    }
    return {};
}

void SamplesModel::update()
{
    did_update(Model::InvalidateAllIndexes);
}
