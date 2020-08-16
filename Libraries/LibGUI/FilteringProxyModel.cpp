/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibGUI/FilteringProxyModel.h>

namespace GUI {

ModelIndex FilteringProxyModel::index(int row, int column, const ModelIndex& parent_index) const
{
    int parent_row = parent_index.row();
    if (!parent_index.is_valid())
        parent_row = 0;

    return create_index(parent_row + row, column);
}

int FilteringProxyModel::row_count(const ModelIndex&) const
{
    return m_matching_indices.size();
}

int FilteringProxyModel::column_count(const ModelIndex& index) const
{
    if (!index.is_valid())
        return {};

    if ((size_t)index.row() > m_matching_indices.size() || index.row() < 0)
        return 0;

    return m_model.column_count(m_matching_indices[index.row()]);
}

Variant FilteringProxyModel::data(const ModelIndex& index, ModelRole role) const
{
    if (!index.is_valid())
        return {};

    if ((size_t)index.row() > m_matching_indices.size() || index.row() < 0)
        return 0;

    return m_matching_indices[index.row()].data(role);
}

void FilteringProxyModel::update()
{
    m_model.update();
    filter();
    did_update();
}

void FilteringProxyModel::filter()
{
    m_matching_indices.clear();

    Function<void(ModelIndex&)> add_matching = [&](ModelIndex& parent_index) {
        for (auto i = 0; i < m_model.row_count(parent_index); ++i) {
            auto index = m_model.index(i, 0, parent_index);
            if (!index.is_valid())
                continue;

            auto filter_matches = m_model.data_matches(index, m_filter_term);
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

void FilteringProxyModel::set_filter_term(const StringView& term)
{
    if (m_filter_term == term)
        return;
    m_filter_term = term;
    update();
}

ModelIndex FilteringProxyModel::map(const ModelIndex& index) const
{
    if (!index.is_valid())
        return {};

    auto row = index.row();
    if (m_matching_indices.size() > (size_t)row)
        return m_matching_indices[row];

    return {};
}

}
