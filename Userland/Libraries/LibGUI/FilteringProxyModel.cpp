/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

    return m_model->column_count(m_matching_indices[index.row()]);
}

Variant FilteringProxyModel::data(ModelIndex const& index, ModelRole role) const
{
    if (!index.is_valid())
        return {};

    if ((size_t)index.row() > m_matching_indices.size() || index.row() < 0)
        return {};

    // FIXME: Support hierarchical models (with a non-empty index.parent()).
    auto underlying_index = m_model->index(m_matching_indices[index.row()].row(), index.column(), {});
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

            auto filter_matches = m_model->data_matches(index, m_filter_term);
            bool matches = filter_matches == TriState::True;
            if (filter_matches == TriState::Unknown) {
                auto data = index.data();
                if (data.is_string() && data.as_string().contains(m_filter_term))
                    matches = true;
            }
            if (matches)
                m_matching_indices.append(index);

            add_matching(index);
        }
    };

    ModelIndex parent_index;
    add_matching(parent_index);
}

void FilteringProxyModel::set_filter_term(StringView term)
{
    if (m_filter_term == term)
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
        return m_matching_indices[row];

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
