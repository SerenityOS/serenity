/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InboxModel.h"
#include <LibGfx/Font/FontDatabase.h>

InboxModel::InboxModel(Vector<InboxEntry> entries)
    : m_entries(move(entries))
{
}

MailStatus InboxModel::mail_status(int row)
{
    return m_entries[row].status;
}

void InboxModel::set_mail_status(int row, MailStatus status)
{
    m_entries[row].status = status;
    did_update(DontInvalidateIndices);
}

int InboxModel::row_count(GUI::ModelIndex const&) const
{
    return m_entries.size();
}

ErrorOr<String> InboxModel::column_name(int column_index) const
{
    switch (column_index) {
    case Date:
        return "Date"_string;
    case Column::From:
        return "From"_string;
    case Subject:
        return "Subject"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant InboxModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto& value = m_entries[index.row()];
    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::Date)
            return value.date;
        if (index.column() == Column::From)
            return value.from;
        if (index.column() == Column::Subject)
            return value.subject;
    }
    if (role == GUI::ModelRole::TextAlignment) {
        if (index.column() == Column::Date)
            return Gfx::TextAlignment::CenterRight;
    }
    if (role == GUI::ModelRole::Font && value.status == MailStatus::Unseen)
        return Gfx::FontDatabase::default_font().bold_variant();
    if (role == static_cast<GUI::ModelRole>(InboxModelCustomRole::Sequence))
        return value.sequence_number;
    return {};
}
