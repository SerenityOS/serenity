/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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

int IndividualSampleModel::row_count(GUI::ModelIndex const&) const
{
    auto const& event = m_profile.events().at(m_event_index);
    return event.frames.size();
}

int IndividualSampleModel::column_count(GUI::ModelIndex const&) const
{
    return Column::__Count;
}

ErrorOr<String> IndividualSampleModel::column_name(int column) const
{
    switch (column) {
    case Column::Address:
        return "Address"_string;
    case Column::ObjectName:
        return "Object"_string;
    case Column::Symbol:
        return "Symbol"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant IndividualSampleModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto const& event = m_profile.events().at(m_event_index);
    auto const& frame = event.frames[event.frames.size() - index.row() - 1];

    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::Address)
            return ByteString::formatted("{:p}", frame.address);

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
