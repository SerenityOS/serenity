/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "IndividualSampleModel.h"
#include "Profile.h"
#include <AK/StringBuilder.h>
#include <stdio.h>

namespace Profiler {

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
            return String::formatted("{:p}", frame.address);

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

}
