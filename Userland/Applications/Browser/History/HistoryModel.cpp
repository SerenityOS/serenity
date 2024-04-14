/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HistoryModel.h"
#include <AK/FuzzyMatch.h>

namespace Browser {

void HistoryModel::set_items(Vector<URLTitlePair> items)
{
    begin_insert_rows({}, m_entries.size(), m_entries.size());
    m_entries = move(items);
    end_insert_rows();

    did_update(DontInvalidateIndices);
}

void HistoryModel::clear_items()
{
    begin_insert_rows({}, m_entries.size(), m_entries.size());
    m_entries.clear();
    end_insert_rows();

    did_update(DontInvalidateIndices);
}

int HistoryModel::row_count(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return m_entries.size();
    return 0;
}

ErrorOr<String> HistoryModel::column_name(int column) const
{
    switch (column) {
    case Column::Title:
        return "Title"_string;
    case Column::URL:
        return "URL"_string;
    default:
        VERIFY_NOT_REACHED();
    }

    return String {};
}

GUI::ModelIndex HistoryModel::index(int row, int column, GUI::ModelIndex const&) const
{
    if (static_cast<size_t>(row) < m_entries.size())
        return create_index(row, column, &m_entries.at(row));
    return {};
}

GUI::Variant HistoryModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role != GUI::ModelRole::Display)
        return {};

    auto const& history_entry = m_entries[index.row()];

    switch (index.column()) {
    case Column::Title:
        return history_entry.title;
    case Column::URL:
        return history_entry.url.serialize();
    }

    VERIFY_NOT_REACHED();
}

GUI::Model::MatchResult HistoryModel::data_matches(GUI::ModelIndex const& index, GUI::Variant const& term) const
{
    auto needle = term.as_string();
    if (needle.is_empty())
        return { TriState::True };

    auto const& history_entry = m_entries[index.row()];
    auto haystack = ByteString::formatted("{} {}", history_entry.title, history_entry.url.serialize());
    auto match_result = fuzzy_match(needle, haystack);
    if (match_result.score > 0)
        return { TriState::True, match_result.score };
    return { TriState::False };
}

}
