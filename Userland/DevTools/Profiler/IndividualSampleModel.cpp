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

#include "IndividualSampleModel.h"
#include "Profile.h"
#include <AK/StringBuilder.h>
#include <stdio.h>

IndividualSampleModel::IndividualSampleModel(Profile& profile, size_t event_index)
    : m_profile(profile)
    , m_event_index(event_index)
{
}

IndividualSampleModel::~IndividualSampleModel()
{
}

int IndividualSampleModel::row_count(const GUI::ModelIndex&) const
{
    auto& event = m_profile.events().at(m_event_index);
    return event.frames.size();
}

int IndividualSampleModel::column_count(const GUI::ModelIndex&) const
{
    return Column::__Count;
}

String IndividualSampleModel::column_name(int column) const
{
    switch (column) {
    case Column::Address:
        return "Address";
    case Column::ObjectName:
        return "Object";
    case Column::Symbol:
        return "Symbol";
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant IndividualSampleModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto& event = m_profile.events().at(m_event_index);
    auto& frame = event.frames[event.frames.size() - index.row() - 1];

    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::Address)
            return String::formatted("{:08x}", frame.address);

        if (index.column() == Column::Symbol) {
            return frame.symbol;
        }

        if (index.column() == Column::ObjectName) {
            return frame.object_name;
        }
        return {};
    }
    return {};
}

void IndividualSampleModel::update()
{
    did_update(Model::InvalidateAllIndexes);
}
