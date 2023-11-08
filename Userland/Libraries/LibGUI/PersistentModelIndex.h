/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <LibGUI/Model.h>
#include <LibGUI/ModelIndex.h>

namespace GUI {

/// A PersistentHandle is an internal data structure used to keep track of the
/// target of multiple PersistentModelIndex instances.
class PersistentHandle : public Weakable<PersistentHandle> {
    friend Model;
    friend PersistentModelIndex;
    friend AK::Traits<GUI::PersistentModelIndex>;

    PersistentHandle(ModelIndex const& index)
        : m_index(index)
    {
    }

    ModelIndex m_index;
};

class PersistentModelIndex {
public:
    PersistentModelIndex() = default;
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
    friend AK::Traits<GUI::PersistentModelIndex>;

    WeakPtr<PersistentHandle> m_handle;
};

}

namespace AK {

template<>
struct Formatter<GUI::PersistentModelIndex> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, GUI::PersistentModelIndex const& value)
    {
        return Formatter<FormatString>::format(builder, "PersistentModelIndex({},{},{})"sv, value.row(), value.column(), value.internal_data());
    }
};

template<>
struct Traits<GUI::PersistentModelIndex> : public DefaultTraits<GUI::PersistentModelIndex> {
    static unsigned hash(const GUI::PersistentModelIndex& index)
    {
        if (index.has_valid_handle())
            return Traits<GUI::ModelIndex>::hash(index.m_handle->m_index);
        return 0;
    }
};

}
