/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ViewModel.h"
#include <AK/QuickSort.h>

namespace GUI {

ViewModel::Mapping* ViewModel::mapping_for_index(ModelIndex const& proxy_index) const
{
    VERIFY(proxy_index.is_valid());
    VERIFY(proxy_index.model() == this);
    VERIFY(proxy_index.internal_data());

    auto mapping = static_cast<Mapping*>(proxy_index.internal_data());
    // FIXME: This is an extremely naive method of checking if the pointer we're
    // holding actually points to an existing mapping before dereferencing it.
    // A more performant but memory-heavy way would be to keep a HashMap of
    // active mapping pointers which we clean up after. I couldn't find a good
    // solution to this, so feel free to clean it up if you did. :^)
    for (auto& entry : m_mappings) {
        if (entry.value.ptr() == mapping) {
            return mapping;
        }
    }

    // We probably got here because the mapping for the given index is gone.
    return nullptr;
}

ModelIndex ViewModel::source_index_from_proxy(ModelIndex const& proxy_index) const
{
    //dbgln("ViewModel::source_index_from_proxy Trying to obtain source for: {}", proxy_index);
    if (!proxy_index.is_valid()) {
        // dbgln("ViewModel::source_index_from_proxy The index wasn't valid");
        return {};
    }

    auto mapping = mapping_for_index(proxy_index);
    if (mapping == nullptr)
        return {};

    // dbgln("ViewModel::source_index_from_proxy Got mapping at {}", mapping);
    auto target_persistent_index = mapping->proxied_rows.at(proxy_index.row());
    // dbgln("ViewModel::source_index_from_proxy Got persistent index {}", target_persistent_index);
    return m_source->index(target_persistent_index.row(), proxy_index.column(), mapping->source_parent);
}

ModelIndex ViewModel::proxy_index_from_source(ModelIndex const& source_index) const
{
    //dbgln("ViewModel::proxy_index_from_source Trying to obtain proxy for: {}", source_index);
    if (!source_index.is_valid()) {
        // dbgln("ViewModel::proxy_index_from_source The index wasn't valid");
        return {};
    }

    auto mapping = const_cast<ViewModel*>(this)->get_or_create_mapping(source_index.parent());
    // dbgln("ViewModel::proxy_index_from_source Got mapping at {}", mapping);

    for (size_t i = 0; i < mapping->proxied_rows.size(); i++) {
        auto& proxy_index = mapping->proxied_rows[i];
        if (proxy_index.row() == source_index.row()) {
            VERIFY(proxy_index.is_valid());
            auto ret = create_index(i, source_index.column(), mapping);
            //dbgln("ViewModel::proxy_index_from_source Returning proxy index: {}", ret);
            return ret;
        }
    }

    // dbgln("ViewModel::proxy_index_from_source Couldn't find any valid item");
    return {};
}

ModelIndex ViewModel::index_from_source_parent(int row, int column, ModelIndex const& source_parent) const
{
    auto it = m_mappings.find(source_parent);
    if (it == m_mappings.end()) {
        return {};
    }

    auto& mapping = *it->value;
    return create_index(row, column, &mapping);
}

ViewModel::Mapping* ViewModel::get_or_create_mapping(const ModelIndex& source_parent)
{
    //dbgln("ViewModel::get_or_create_mapping Getting mapping for {}", source_parent);

    // Do we already have a mapping for this parent?
    auto it = m_mappings.find(source_parent);
    if (it != m_mappings.end()) {
        return it->value.ptr();
    }

    // dbgln("ViewModel::get_or_create_mapping Making the mapping.");
    auto mapping = make<Mapping>();
    mapping->source_parent = source_parent;

    int row_count = m_source->row_count(source_parent);
    VERIFY(row_count >= 0);
    mapping->proxied_rows.ensure_capacity(row_count);

    // dbgln("ViewModel::get_or_create_mapping Inserting {} rows", row_count);
    for (int i = 0; i < row_count; i++) {
        mapping->proxied_rows.empend(m_source->index(i, tree_column(), source_parent));
    }

    // dbgln("ViewModel::get_or_create_mapping sorting/filtering");
    filter_mapping(*mapping);
    sort_mapping(*mapping);

    // Do we have a mapping for this parent's parent? If not, we're most likely
    // going to end up needing it anyway, so build it as well.
    if (source_parent.is_valid()) {
        // dbgln("ViewModel::get_or_create_mapping Also looking at grandparent...");
        auto source_grandparent = source_parent.parent();
        get_or_create_mapping(source_grandparent);
    }

    // dbgln("ViewModel::get_or_create_mapping Assigning to m_mappings. Should be able to find it now.");
    auto ptr = mapping.ptr();
    m_mappings.set(source_parent, move(mapping));

    return ptr;
}

ViewModel::ViewModel(NonnullRefPtr<Model> source)
    : m_source(move(source))
{
    m_source->register_client(*this);
}

ViewModel::~ViewModel()
{
    m_source->unregister_client(*this);
}

int ViewModel::row_count(ModelIndex const& proxy_parent) const
{
    return m_source->row_count(source_index_from_proxy(proxy_parent));
}

int ViewModel::column_count(ModelIndex const& proxy_parent) const
{
    return m_source->column_count(source_index_from_proxy(proxy_parent));
}

String ViewModel::column_name(int index) const
{
    return m_source->column_name(index);
}

Variant ViewModel::data(ModelIndex const& proxy_index, ModelRole role) const
{
    return m_source->data(source_index_from_proxy(proxy_index), role);
}

TriState ViewModel::data_matches(ModelIndex const& proxy_index, Variant const& data) const
{
    return m_source->data_matches(source_index_from_proxy(proxy_index), data);
}

ModelIndex ViewModel::parent_index(ModelIndex const& proxy_index) const
{
    if (!proxy_index.is_valid())
        return {};

    auto mapping = mapping_for_index(proxy_index);
    if (mapping == nullptr)
        return {};

    return proxy_index_from_source(mapping->source_parent);
}

ModelIndex ViewModel::index(int row, int column, ModelIndex const& proxy_parent) const
{
    if (row < 0 || column < 0)
        return {};

    auto source_parent = source_index_from_proxy(proxy_parent);
    auto mapping = const_cast<ViewModel*>(this)->get_or_create_mapping(source_parent);

    if (row >= m_source->row_count(source_parent) || column >= m_source->column_count(source_parent))
        return {};
    return create_index(row, column, mapping);
}

bool ViewModel::is_editable(ModelIndex const& proxy_index) const
{
    return m_source->is_editable(source_index_from_proxy(proxy_index));
}

bool ViewModel::is_searchable() const
{
    return m_source->is_searchable();
}

void ViewModel::set_data(ModelIndex const& proxy_index, Variant const& data)
{
    m_source->set_data(source_index_from_proxy(proxy_index), data);
}

int ViewModel::tree_column() const
{
    return m_source->tree_column();
}

bool ViewModel::accepts_drag(ModelIndex const& proxy_index, Vector<String> const& mime_types) const
{
    return m_source->accepts_drag(source_index_from_proxy(proxy_index), mime_types);
}

Vector<ModelIndex, 1> ViewModel::matches(StringView const& term, unsigned flags, ModelIndex const& proxy_parent)
{
    auto source_result = m_source->matches(term, flags, source_index_from_proxy(proxy_parent));
    Vector<ModelIndex, 1> proxy_result;

    for (auto& source_index : source_result) {
        // FIXME: This feels kind of slow... A way to improve it could be to
        // create a HashTable of rows that are available.
        proxy_result.append(proxy_index_from_source(source_index));
    }

    return proxy_result;
}

void ViewModel::invalidate()
{
    m_mappings.clear();
    Model::invalidate();
    m_source->invalidate();
}

bool ViewModel::is_column_sortable(int column_index) const
{
    return m_source->is_column_sortable(column_index);
}

void ViewModel::model_did_update(unsigned flags)
{
    dbgln("ViewModel::model_did_update With flags: {}", flags);

    if (flags & UpdateFlag::InvalidateAllIndices) {
        m_mappings.clear();
    } else {
        update_mappings();
        m_mappings.clear();
    }

    Model::did_update(flags);
}
void ViewModel::model_did_insert_rows(ModelIndex const& parent, int first, int last)
{
    dbgln("ViewModel::model_did_insert_rows {} {}-{}", parent, first, last);
}

void ViewModel::model_did_move_rows(ModelIndex const& source_parent, int first, int last, ModelIndex const& target_parent, int target_index)
{
    dbgln("ViewModel::model_did_move_rows from {} {}-{} to {} {}", source_parent, first, last, target_parent, target_index);
}

void ViewModel::model_did_delete_rows(ModelIndex const& parent, int first, int last)
{
    dbgln("ViewModel::model_did_delete_rows {} {}-{}", parent, first, last);
}

bool ViewModel::less_than(ModelIndex const& a, ModelIndex const& b)
{
    auto a_data = a.data(m_sort_role);
    auto b_data = b.data(m_sort_role);
    if (a_data.is_string() && b_data.is_string()) {
        if (m_case_sensitivity == CaseSensitivity::CaseSensitive)
            return a_data.as_string() < b_data.as_string();
        else
            return a_data.as_string().to_lowercase() < b_data.as_string().to_lowercase();
    }
    return a_data < b_data;
}

void ViewModel::sort_mapping(Mapping& mapping)
{
    if (m_sort_column == -1) {
        // -1 means default order (no sorting).
        quick_sort(mapping.proxied_rows, [](auto& a, auto& b) -> bool {
            //dbgln("Sorting {} {}", a, b);
            return a.row() < b.row();
        });
        return;
    }

    // dbgln("ViewModel::sort_mapping Sorting based on column {}", m_sort_column);
    // TODO: granular updates!
    quick_sort(mapping.proxied_rows, [&](auto a, auto b) {
        bool is_less_than = less_than(m_source->index(a.row(), m_sort_column, mapping.source_parent), m_source->index(b.row(), m_sort_column, mapping.source_parent));
        return m_sort_order == SortOrder::Ascending ? is_less_than : !is_less_than;
    });
    // dbgln("ViewModel::sort_mapping Sorting done");
}

void ViewModel::filter_mapping(Mapping& mapping)
{
    if (m_filter_term.is_empty())
        return;

    auto old_proxied_rows = mapping.proxied_rows;

    // NOTE: filter_mapping is depth-first.
    mapping.proxied_rows.remove_all_matching([&](PersistentModelIndex& source_row) {
        bool child_is_nonempty = false;

        auto it = m_mappings.find(m_source->index(source_row.row(), tree_column(), mapping.source_parent));
        if (it != m_mappings.end()) {
            Mapping& child_mapping = *it->value;
            filter_mapping(child_mapping);

            child_is_nonempty = !child_mapping.proxied_rows.is_empty();
        }

        // FIXME: This behavior is ported as-is from FilteringProxyModel. Do we
        // really want to filter column 0 everytime? Does data_matches care
        // about the column?
        auto index_to_filter = m_source->index(source_row.row(), 0, mapping.source_parent);
        auto filter_matches = m_source->data_matches(index_to_filter, m_filter_term);
        bool matches = filter_matches == TriState::True;
        if (filter_matches == TriState::Unknown) {
            // Default behavior: Try string comparison.
            auto data = index_to_filter.data();
            // FIXME: Should we care about case sensitivity here?
            if (data.is_string() && data.as_string().contains(m_filter_term))
                matches = true;
        }

        // NOTE: We don't want to hide the parent index if we found a match in a
        // child, since that would hide the child as well, so if the child
        // mapping is not empty, we must also stay visible.  This behavior can
        // be modified in the future if flattening the matched items is desired.
        return matches || child_is_nonempty;
    });
}

void ViewModel::update_mappings()
{
    // TODO
}

Vector<ViewModel::SourceProxyPair> ViewModel::backup_persistent_indices(Mapping const& mapping)
{
    Vector<SourceProxyPair> indices;
    indices.ensure_capacity(mapping.proxied_rows.size());

    for (auto& source_persistent_index : mapping.proxied_rows) {
        ModelIndex source_index = source_persistent_index;
        indices.empend(source_index, proxy_index_from_source(source_index));
    }

    return indices;
}

void ViewModel::update_persistent_indices(Vector<SourceProxyPair> const& saved_indices)
{
    Vector<ModelIndex> old_indices;
    Vector<ModelIndex> new_indices;

    old_indices.ensure_capacity(saved_indices.size());
    new_indices.ensure_capacity(saved_indices.size());

    for (auto& index_pair : saved_indices) {
        old_indices.append(index_pair.proxy_index);

        auto proxy_index = proxy_index_from_source(index_pair.source_index);
        if (!proxy_index.is_valid()) {
            // The index is gone (filtered out probably).
            new_indices.empend();
        } else {
            new_indices.append(proxy_index);
        }
    }

    change_persistent_index_list(old_indices, new_indices);
}

// NOTE: sort_impl/filter_impl functions do the relevant heavy lifting, while
// sort/filter provide an API to ViewModel users and automatically handle
// book-keeping.

void ViewModel::sort_impl()
{
    for (auto& entry : m_mappings) {
        sort_mapping(*entry.value);
    }
}

void ViewModel::filter_impl()
{
    auto it = m_mappings.find(ModelIndex());
    if (it == m_mappings.end()) {
        // FIXME: Can we ever not have the parent of root available?
        return;
    }

    filter_mapping(*it->value);
    sort_impl();
}

void ViewModel::sort(int column, SortOrder sort_order)
{
    if (m_sort_column == column && m_sort_order == sort_order)
        return;
    m_sort_column = column;
    m_sort_order = sort_order;

    sort_impl();
    did_update(UpdateFlag::DontInvalidateIndices);
}

void ViewModel::filter(String term)
{
    if (m_filter_term == term)
        return;
    m_filter_term = term;

    filter_impl();
    did_update(UpdateFlag::DontInvalidateIndices);
}

void ViewModel::set_sort_role(ModelRole role)
{
    if (m_sort_role == role)
        return;
    m_sort_role = role;
    if (m_sort_column == -1)
        return;

    sort_impl();
}

void ViewModel::set_case_sensitivity(CaseSensitivity sensitivity)
{
    if (m_case_sensitivity == sensitivity)
        return;
    m_case_sensitivity = sensitivity;
    if (m_sort_column == -1)
        return;

    sort_impl();
}
}
