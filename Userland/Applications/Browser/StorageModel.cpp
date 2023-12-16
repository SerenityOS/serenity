/*
 * Copyright (c) 2022, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "StorageModel.h"

#include <AK/FuzzyMatch.h>

namespace Browser {

void StorageModel::set_items(OrderedHashMap<String, String> map)
{
    begin_insert_rows({}, m_local_storage_entries.size(), m_local_storage_entries.size());
    m_local_storage_entries = move(map);
    end_insert_rows();

    did_update(DontInvalidateIndices);
}

void StorageModel::clear_items()
{
    begin_insert_rows({}, m_local_storage_entries.size(), m_local_storage_entries.size());
    m_local_storage_entries.clear();
    end_insert_rows();

    did_update(DontInvalidateIndices);
}

int StorageModel::row_count(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return m_local_storage_entries.size();
    return 0;
}

ErrorOr<String> StorageModel::column_name(int column) const
{
    switch (column) {
    case Column::Key:
        return "Key"_string;
    case Column::Value:
        return "Value"_string;
    case Column::__Count:
        return String {};
    }

    return String {};
}

GUI::ModelIndex StorageModel::index(int row, int column, GUI::ModelIndex const&) const
{
    if (static_cast<size_t>(row) < m_local_storage_entries.size())
        return create_index(row, column, NULL);
    return {};
}

GUI::Variant StorageModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role != GUI::ModelRole::Display)
        return {};

    auto const& keys = m_local_storage_entries.keys();
    auto const& local_storage_key = keys[index.row()];
    auto const& local_storage_value = m_local_storage_entries.get(local_storage_key).value_or({});

    switch (index.column()) {
    case Column::Key:
        return local_storage_key;
    case Column::Value:
        return local_storage_value;
    }

    VERIFY_NOT_REACHED();
}

GUI::Model::MatchResult StorageModel::data_matches(GUI::ModelIndex const& index, GUI::Variant const& term) const
{
    auto needle = term.as_string();
    if (needle.is_empty())
        return { TriState::True };

    auto const& keys = m_local_storage_entries.keys();
    auto const& local_storage_key = keys[index.row()];
    auto const& local_storage_value = m_local_storage_entries.get(local_storage_key).value_or({});

    auto haystack = ByteString::formatted("{} {}", local_storage_key, local_storage_value);
    auto match_result = fuzzy_match(needle, haystack);
    if (match_result.score > 0)
        return { TriState::True, match_result.score };
    return { TriState::False };
}

}
