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

String InboxModel::column_name(int column_index) const
{
    switch (column_index) {
    case Column::From:
        return "From";
    case Subject:
        return "Subject";
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
    }
    return {};
}
