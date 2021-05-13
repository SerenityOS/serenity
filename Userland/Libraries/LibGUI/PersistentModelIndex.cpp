/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/PersistentModelIndex.h>

namespace GUI {

PersistentModelIndex::PersistentModelIndex(ModelIndex const& index)
{
    if (!index.is_valid())
        return;

    auto* model = const_cast<Model*>(index.model());
    m_handle = model->register_persistent_index({}, index);
}

int PersistentModelIndex::row() const
{
    if (!has_valid_handle())
        return -1;
    return m_handle->m_index.row();
}

int PersistentModelIndex::column() const
{
    if (!has_valid_handle())
        return -1;
    return m_handle->m_index.column();
}

PersistentModelIndex PersistentModelIndex::parent() const
{
    if (!has_valid_handle())
        return {};
    return { m_handle->m_index.parent() };
}

PersistentModelIndex PersistentModelIndex::sibling_at_column(int column) const
{
    if (!has_valid_handle())
        return {};

    return { m_handle->m_index.sibling_at_column(column) };
}

Variant PersistentModelIndex::data(ModelRole role) const
{
    if (!has_valid_handle())
        return {};
    return { m_handle->m_index.data(role) };
}

PersistentModelIndex::operator ModelIndex() const
{
    if (!has_valid_handle())
        return {};
    else
        return m_handle->m_index;
}

bool PersistentModelIndex::operator==(PersistentModelIndex const& other) const
{
    bool is_this_valid = has_valid_handle();
    bool is_other_valid = other.has_valid_handle();

    if (!is_this_valid && !is_other_valid)
        return true;
    if (is_this_valid != is_other_valid)
        return false;

    return m_handle->m_index == other.m_handle->m_index;
}

bool PersistentModelIndex::operator!=(PersistentModelIndex const& other) const
{
    return !(*this == other);
}

bool PersistentModelIndex::operator==(ModelIndex const& other) const
{
    if (!has_valid_handle()) {
        return !other.is_valid();
    }

    return m_handle->m_index == other;
}

bool PersistentModelIndex::operator!=(ModelIndex const& other) const
{
    return !(*this == other);
}

}
