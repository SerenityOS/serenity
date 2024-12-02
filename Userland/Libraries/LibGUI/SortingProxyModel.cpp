/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibGUI/AbstractView.h>
#include <LibGUI/SortingProxyModel.h>

namespace GUI {

SortingProxyModel::SortingProxyModel(NonnullRefPtr<Model> source)
    : m_source(move(source))
{
    m_source->register_client(*this);
    update_sort();
}

SortingProxyModel::~SortingProxyModel()
{
    m_source->unregister_client(*this);
}

void SortingProxyModel::invalidate()
{
    source().invalidate();
    Model::invalidate();
}

void SortingProxyModel::update_sort(unsigned flags)
{
    if (flags == UpdateFlag::DontInvalidateIndices) {
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
    update_sort(flags);
}

bool SortingProxyModel::accepts_drag(ModelIndex const& proxy_index, Core::MimeData const& mime_data) const
{
    return source().accepts_drag(map_to_source(proxy_index), mime_data);
}

int SortingProxyModel::row_count(ModelIndex const& proxy_index) const
{
    return source().row_count(map_to_source(proxy_index));
}

int SortingProxyModel::column_count(ModelIndex const& proxy_index) const
{
    return source().column_count(map_to_source(proxy_index));
}

ModelIndex SortingProxyModel::map_to_source(ModelIndex const& proxy_index) const
{
    if (!proxy_index.is_valid())
        return {};

    VERIFY(proxy_index.model() == this);
    VERIFY(proxy_index.internal_data());

    auto& index_mapping = *static_cast<Mapping*>(proxy_index.internal_data());
    auto it = m_mappings.find(index_mapping.source_parent);
    VERIFY(it != m_mappings.end());

    auto& mapping = *it->value;
    if (static_cast<size_t>(proxy_index.row()) >= mapping.source_rows.size() || proxy_index.column() >= column_count())
        return {};
    int source_row = mapping.source_rows[proxy_index.row()];
    int source_column = proxy_index.column();
    return source().index(source_row, source_column, it->key);
}

ModelIndex SortingProxyModel::map_to_proxy(ModelIndex const& source_index) const
{
    if (!source_index.is_valid())
        return {};

    VERIFY(source_index.model() == m_source);

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

ErrorOr<String> SortingProxyModel::column_name(int column) const
{
    return source().column_name(column);
}

Variant SortingProxyModel::data(ModelIndex const& proxy_index, ModelRole role) const
{
    return source().data(map_to_source(proxy_index), role);
}

StringView SortingProxyModel::drag_data_type() const
{
    return source().drag_data_type();
}

bool SortingProxyModel::less_than(ModelIndex const& index1, ModelIndex const& index2) const
{
    auto data1 = index1.data(m_sort_role);
    auto data2 = index2.data(m_sort_role);
    if (data1.is_string() && data2.is_string())
        return data1.as_string().to_lowercase() < data2.as_string().to_lowercase();
    return data1 < data2;
}

ModelIndex SortingProxyModel::index(int row, int column, ModelIndex const& parent) const
{
    if (row < 0 || column < 0)
        return {};

    auto source_parent = map_to_source(parent);
    const_cast<SortingProxyModel*>(this)->build_mapping(source_parent);

    auto it = m_mappings.find(source_parent);
    VERIFY(it != m_mappings.end());
    auto& mapping = *it->value;
    if (row >= static_cast<int>(mapping.source_rows.size()) || column >= column_count())
        return {};
    return create_index(row, column, &mapping);
}

ModelIndex SortingProxyModel::parent_index(ModelIndex const& proxy_index) const
{
    if (!proxy_index.is_valid())
        return {};

    VERIFY(proxy_index.model() == this);
    VERIFY(proxy_index.internal_data());

    auto& index_mapping = *static_cast<Mapping*>(proxy_index.internal_data());
    auto it = m_mappings.find(index_mapping.source_parent);
    VERIFY(it != m_mappings.end());

    return map_to_proxy(it->value->source_parent);
}

void SortingProxyModel::sort_mapping(Mapping& mapping, int column, SortOrder sort_order)
{
    auto old_source_rows = mapping.source_rows;

    int row_count = source().row_count(mapping.source_parent);
    mapping.source_rows.resize(row_count);
    mapping.proxy_rows.resize(row_count);

    if (column == -1) {
        for (int i = 0; i < row_count; ++i) {
            mapping.source_rows[i] = i;
            mapping.proxy_rows[i] = i;
        }
        return;
    }

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
        // Update the view's selection.
        view.selection().change_from_model({}, [&](ModelSelection& selection) {
            Vector<ModelIndex> selected_indices_in_source;
            Vector<ModelIndex> stale_indices_in_selection;
            selection.for_each_index([&](ModelIndex const& index) {
                if (index.parent() == mapping.source_parent) {
                    stale_indices_in_selection.append(index);
                    selected_indices_in_source.append(source().index(old_source_rows[index.row()], index.column(), mapping.source_parent));
                }
            });

            for (auto& index : stale_indices_in_selection) {
                selection.remove(index);
            }

            for (auto& index : selected_indices_in_source) {
                for (size_t i = 0; i < mapping.source_rows.size(); ++i) {
                    if (mapping.source_rows[i] == index.row()) {
                        auto new_source_index = this->index(i, index.column(), mapping.source_parent);
                        selection.add(new_source_index);
                        // Update the view's cursor.
                        auto cursor = view.cursor_index();
                        if (cursor.is_valid() && cursor.parent() == mapping.source_parent)
                            view.set_cursor(new_source_index, AbstractView::SelectionUpdate::None, false);
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

    did_update(UpdateFlag::DontInvalidateIndices);
}

SortingProxyModel::InternalMapIterator SortingProxyModel::build_mapping(ModelIndex const& source_parent)
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

bool SortingProxyModel::is_editable(ModelIndex const& proxy_index) const
{
    return source().is_editable(map_to_source(proxy_index));
}

void SortingProxyModel::set_data(ModelIndex const& proxy_index, Variant const& data)
{
    source().set_data(map_to_source(proxy_index), data);
}

bool SortingProxyModel::is_searchable() const
{
    return source().is_searchable();
}

Vector<ModelIndex> SortingProxyModel::matches(StringView searching, unsigned flags, ModelIndex const& proxy_index)
{
    auto found_indices = source().matches(searching, flags, map_to_source(proxy_index));
    for (size_t i = 0; i < found_indices.size(); i++)
        found_indices[i] = map_to_proxy(found_indices[i]);
    return found_indices;
}

}
