/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InboxModel.h"

InboxModel::InboxModel(Vector<InboxEntry> entries)
    : m_entries(move(entries))
{
}

int InboxModel::row_count(GUI::ModelIndex const&) const
{
    return m_entries.size();
}

ErrorOr<String> InboxModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::From:
        return "From"_string;
    case Subject:
        return "Subject"_string;
    case Date:
        return "Date"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant InboxModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto& value = m_entries[index.row()];
    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::From)
            return value.from;
        if (index.column() == Column::Subject)
            return value.subject;
        if (index.column() == Column::Date)
            return value.date;
    }
    if (role == GUI::ModelRole::TextAlignment) {
        if (index.column() == Column::Date)
            return Gfx::TextAlignment::CenterRight;
    }
    return {};
}
