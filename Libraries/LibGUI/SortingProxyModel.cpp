/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/QuickSort.h>
#include <LibGUI/AbstractView.h>
#include <LibGUI/SortingProxyModel.h>

namespace GUI {

SortingProxyModel::SortingProxyModel(NonnullRefPtr<Model> source)
    : m_source(move(source))
{
    m_source->register_client(*this);
    invalidate();
}

SortingProxyModel::~SortingProxyModel()
{
    m_source->unregister_client(*this);
}

void SortingProxyModel::invalidate(unsigned int flags)
{
    if (flags == UpdateFlag::DontInvalidateIndexes) {
        sort(m_last_key_column, m_last_sort_order);
    } else {
        m_mappings.clear();

        // FIXME: This is really harsh, but without precise invalidation, not much we can do.
        for_each_view([&](auto& view) {
            view.set_cursor({}, AbstractView::SelectionUpdate::None);
            view.selection().clear();
        });
    }
    did_update(flags);
}

void SortingProxyModel::model_did_update(unsigned flags)
{
    invalidate(flags);
}

int SortingProxyModel::row_count(const ModelIndex& proxy_index) const
{
    return source().row_count(map_to_source(proxy_index));
}

int SortingProxyModel::column_count(const ModelIndex& proxy_index) const
{
    return source().column_count(map_to_source(proxy_index));
}

ModelIndex SortingProxyModel::map_to_source(const ModelIndex& proxy_index) const
{
    if (!proxy_index.is_valid())
        return {};

    ASSERT(proxy_index.model() == this);
    ASSERT(proxy_index.internal_data());

    auto& index_mapping = *static_cast<Mapping*>(proxy_index.internal_data());
    auto it = m_mappings.find(index_mapping.source_parent);
    ASSERT(it != m_mappings.end());

    auto& mapping = *it->value;
    if (static_cast<size_t>(proxy_index.row()) >= mapping.source_rows.size() || proxy_index.column() >= column_count())
        return {};
    int source_row = mapping.source_rows[proxy_index.row()];
    int source_column = proxy_index.column();
    return source().index(source_row, source_column, it->key);
}

ModelIndex SortingProxyModel::map_to_proxy(const ModelIndex& source_index) const
{
    if (!source_index.is_valid())
        return {};

    ASSERT(source_index.model() == m_source);

    auto source_parent = source_index.parent();
    auto it = const_cast<SortingProxyModel*>(this)->build_mapping(source_parent);

    auto& mapping = *it->value;

    if (source_index.row() >= static_cast<int>(mapping.proxy_rows.size()) || source_index.column() >= column_count())
        return {};

    int proxy_row = mapping.proxy_rows[source_index.row()];
    int proxy_column = source_index.column();
    if (proxy_row < 0 || proxy_column < 0)
        return {};
    return create_index(proxy_row, proxy_column, &mapping);
}

String SortingProxyModel::column_name(int column) const
{
    return source().column_name(column);
}

Variant SortingProxyModel::data(const ModelIndex& proxy_index, ModelRole role) const
{
    return source().data(map_to_source(proxy_index), role);
}

void SortingProxyModel::update()
{
    source().update();
}

StringView SortingProxyModel::drag_data_type() const
{
    return source().drag_data_type();
}

bool SortingProxyModel::less_than(const ModelIndex& index1, const ModelIndex& index2) const
{
    auto data1 = index1.data(m_sort_role);
    auto data2 = index2.data(m_sort_role);
    if (data1.is_string() && data2.is_string())
        return data1.as_string().to_lowercase() < data2.as_string().to_lowercase();
    return data1 < data2;
}

ModelIndex SortingProxyModel::index(int row, int column, const ModelIndex& parent) const
{
    if (row < 0 || column < 0)
        return {};

    auto source_parent = map_to_source(parent);
    const_cast<SortingProxyModel*>(this)->build_mapping(source_parent);

    auto it = m_mappings.find(source_parent);
    ASSERT(it != m_mappings.end());
    auto& mapping = *it->value;
    if (row >= static_cast<int>(mapping.source_rows.size()) || column >= column_count())
        return {};
    return create_index(row, column, &mapping);
}

ModelIndex SortingProxyModel::parent_index(const ModelIndex& proxy_index) const
{
    if (!proxy_index.is_valid())
        return {};

    ASSERT(proxy_index.model() == this);
    ASSERT(proxy_index.internal_data());

    auto& index_mapping = *static_cast<Mapping*>(proxy_index.internal_data());
    auto it = m_mappings.find(index_mapping.source_parent);
    ASSERT(it != m_mappings.end());

    return map_to_proxy(it->value->source_parent);
}

void SortingProxyModel::sort_mapping(Mapping& mapping, int column, SortOrder sort_order)
{
    if (column == -1) {
        int row_count = source().row_count(mapping.source_parent);
        for (int i = 0; i < row_count; ++i) {
            mapping.source_rows[i] = i;
            mapping.proxy_rows[i] = i;
        }
        return;
    }

    auto old_source_rows = mapping.source_rows;

    int row_count = source().row_count(mapping.source_parent);
    for (int i = 0; i < row_count; ++i)
        mapping.source_rows[i] = i;

    quick_sort(mapping.source_rows, [&](auto row1, auto row2) -> bool {
        bool is_less_than = less_than(source().index(row1, column, mapping.source_parent), source().index(row2, column, mapping.source_parent));
        return sort_order == SortOrder::Ascending ? is_less_than : !is_less_than;
    });

    for (int i = 0; i < row_count; ++i)
        mapping.proxy_rows[mapping.source_rows[i]] = i;

    // FIXME: I really feel like this should be done at the view layer somehow.
    for_each_view([&](AbstractView& view) {
        // Update the view's cursor.
        auto cursor = view.cursor_index();
        if (cursor.is_valid() && cursor.parent() == mapping.source_parent) {
            for (size_t i = 0; i < mapping.source_rows.size(); ++i) {
                if (mapping.source_rows[i] == view.cursor_index().row()) {
                    auto new_source_index = this->index(i, view.cursor_index().column(), mapping.source_parent);
                    view.set_cursor(new_source_index, AbstractView::SelectionUpdate::None, false);
                    break;
                }
            }
        }

        // Update the view's selection.
        view.selection().change_from_model({}, [&](ModelSelection& selection) {
            Vector<ModelIndex> selected_indexes_in_source;
            Vector<ModelIndex> stale_indexes_in_selection;
            selection.for_each_index([&](const ModelIndex& index) {
                if (index.parent() == mapping.source_parent) {
                    stale_indexes_in_selection.append(index);
                    selected_indexes_in_source.append(source().index(old_source_rows[index.row()], index.column(), mapping.source_parent));
                }
            });

            for (auto& index : stale_indexes_in_selection) {
                selection.remove(index);
            }

            for (auto& index : selected_indexes_in_source) {
                for (size_t i = 0; i < mapping.source_rows.size(); ++i) {
                    if (mapping.source_rows[i] == index.row()) {
                        auto new_source_index = this->index(i, index.column(), mapping.source_parent);
                        selection.add(new_source_index);
                        break;
                    }
                }
            }
        });
    });
}

void SortingProxyModel::sort(int column, SortOrder sort_order)
{
    for (auto& it : m_mappings) {
        auto& mapping = *it.value;
        sort_mapping(mapping, column, sort_order);
    }

    m_last_key_column = column;
    m_last_sort_order = sort_order;

    did_update(UpdateFlag::DontInvalidateIndexes);
}

SortingProxyModel::InternalMapIterator SortingProxyModel::build_mapping(const ModelIndex& source_parent)
{
    auto it = m_mappings.find(source_parent);
    if (it != m_mappings.end())
        return it;

    auto mapping = make<Mapping>();

    mapping->source_parent = source_parent;

    int row_count = source().row_count(source_parent);
    mapping->source_rows.resize(row_count);
    mapping->proxy_rows.resize(row_count);

    sort_mapping(*mapping, m_last_key_column, m_last_sort_order);

    if (source_parent.is_valid()) {
        auto source_grand_parent = source_parent.parent();
        build_mapping(source_grand_parent);
    }

    m_mappings.set(source_parent, move(mapping));
    return m_mappings.find(source_parent);
}

bool SortingProxyModel::is_column_sortable(int column_index) const
{
    return source().is_column_sortable(column_index);
}

bool SortingProxyModel::is_editable(const ModelIndex& proxy_index) const
{
    return source().is_editable(map_to_source(proxy_index));
}

void SortingProxyModel::set_data(const ModelIndex& proxy_index, const Variant& data)
{
    source().set_data(map_to_source(proxy_index), data);
}

}
