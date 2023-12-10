/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/InsertionSort.h>
#include <LibGUI/FilteringProxyModel.h>

namespace GUI {

ModelIndex FilteringProxyModel::index(int row, int column, ModelIndex const& parent_index) const
{
    int parent_row = parent_index.row();
    if (!parent_index.is_valid())
        parent_row = 0;

    return create_index(parent_row + row, column);
}

int FilteringProxyModel::row_count(ModelIndex const&) const
{
    return m_matching_indices.size();
}

int FilteringProxyModel::column_count(ModelIndex const& index) const
{
    if (!index.is_valid())
        return m_model->column_count({});

    if ((size_t)index.row() > m_matching_indices.size() || index.row() < 0)
        return 0;

    return m_model->column_count(m_matching_indices[index.row()].index);
}

ErrorOr<String> FilteringProxyModel::column_name(int column) const
{
    return m_model->column_name(column);
}

Variant FilteringProxyModel::data(ModelIndex const& index, ModelRole role) const
{
    if (!index.is_valid())
        return {};

    if ((size_t)index.row() > m_matching_indices.size() || index.row() < 0)
        return {};

    auto matching_index = m_matching_indices[index.row()].index;
    auto underlying_index = m_model->index(matching_index.row(), index.column(), matching_index.parent());
    return underlying_index.data(role);
}

void FilteringProxyModel::invalidate()
{
    filter();
    did_update();
}

void FilteringProxyModel::filter()
{
    m_matching_indices.clear();

    Function<void(ModelIndex&)> add_matching = [&](ModelIndex& parent_index) {
        for (auto i = 0; i < m_model->row_count(parent_index); ++i) {
            auto index = m_model->index(i, 0, parent_index);
            if (!index.is_valid())
                continue;

            auto match_result = m_model->data_matches(index, m_filter_term);
            bool matches = match_result.matched == TriState::True;
            auto score = match_result.score;
            if (match_result.matched == TriState::Unknown) {
                auto data = index.data();
                if (data.is_string() && data.as_string().contains(m_filter_term)) {
                    matches = true;
                    score = 0;
                }
            }
            if (matches)
                m_matching_indices.append({ index, score });

            add_matching(index);
        }
    };

    ModelIndex parent_index;
    add_matching(parent_index);
    if (has_flag(m_filtering_options, FilteringOptions::SortByScore))
        // Use a stable sort, so that indices with equal scores don't swap positions.
        insertion_sort(m_matching_indices, [](auto const& a, auto const& b) { return b.score < a.score; });
}

void FilteringProxyModel::set_filter_term(StringView term)
{
    if (m_filter_term == term && !term.is_empty())
        return;
    m_filter_term = term;
    invalidate();
}

ModelIndex FilteringProxyModel::map(ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};

    auto row = index.row();
    if (m_matching_indices.size() > (size_t)row)
        return m_matching_indices[row].index;

    return {};
}

bool FilteringProxyModel::is_searchable() const
{
    return m_model->is_searchable();
}

Vector<ModelIndex> FilteringProxyModel::matches(StringView searching, unsigned flags, ModelIndex const& index)
{
    auto found_indices = m_model->matches(searching, flags, index);
    for (size_t i = 0; i < found_indices.size(); i++)
        found_indices[i] = map(found_indices[i]);
    return found_indices;
}

}
