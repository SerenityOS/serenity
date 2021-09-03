/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/RefCounted.h>
#include <YAK/WeakPtr.h>
#include <LibGUI/Model.h>
#include <LibGUI/ModelIndex.h>

namespace GUI {

/// A PersistentHandle is an internal data structure used to keep track of the
/// target of multiple PersistentModelIndex instances.
class PersistentHandle : public Weakable<PersistentHandle> {
    friend Model;
    friend PersistentModelIndex;
    friend YAK::Traits<GUI::PersistentModelIndex>;

    PersistentHandle(ModelIndex const& index)
        : m_index(index)
    {
    }

    ModelIndex m_index;
};

class PersistentModelIndex {
public:
    PersistentModelIndex() { }
    PersistentModelIndex(ModelIndex const&);
    PersistentModelIndex(PersistentModelIndex const&) = default;
    PersistentModelIndex(PersistentModelIndex&&) = default;

    PersistentModelIndex& operator=(PersistentModelIndex const&) = default;
    PersistentModelIndex& operator=(PersistentModelIndex&&) = default;

    bool is_valid() const { return has_valid_handle() && m_handle->m_index.is_valid(); }
    bool has_valid_handle() const { return !m_handle.is_null(); }

    int row() const;
    int column() const;
    PersistentModelIndex parent() const;
    PersistentModelIndex sibling_at_column(int column) const;
    Variant data(ModelRole = ModelRole::Display) const;

    void* internal_data() const
    {
        if (has_valid_handle())
            return m_handle->m_index.internal_data();
        else
            return nullptr;
    }

    operator ModelIndex() const;
    bool operator==(PersistentModelIndex const&) const;
    bool operator!=(PersistentModelIndex const&) const;
    bool operator==(ModelIndex const&) const;
    bool operator!=(ModelIndex const&) const;

private:
    friend YAK::Traits<GUI::PersistentModelIndex>;

    WeakPtr<PersistentHandle> m_handle;
};

}

namespace YAK {

template<>
struct Formatter<GUI::PersistentModelIndex> : Formatter<FormatString> {
    void format(FormatBuilder& builder, const GUI::PersistentModelIndex& value)
    {
        return Formatter<FormatString>::format(builder, "PersistentModelIndex({},{},{})", value.row(), value.column(), value.internal_data());
    }
};

template<>
struct Traits<GUI::PersistentModelIndex> : public GenericTraits<GUI::PersistentModelIndex> {
    static unsigned hash(const GUI::PersistentModelIndex& index)
    {
        if (index.has_valid_handle())
            return Traits<GUI::ModelIndex>::hash(index.m_handle->m_index);
        else
            return 0;
    }
};

}
