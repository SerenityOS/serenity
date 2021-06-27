/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/HashTable.h>
#include <AK/Noncopyable.h>
#include <AK/TemporaryChange.h>
#include <AK/Vector.h>
#include <LibGUI/ModelIndex.h>
#include <LibGUI/PersistentModelIndex.h>

namespace GUI {

class ModelSelection {
    AK_MAKE_NONCOPYABLE(ModelSelection);
    AK_MAKE_NONMOVABLE(ModelSelection);

public:
    ModelSelection(AbstractView& view)
        : m_view(view)
    {
    }

    int size() const { return m_indices.size(); }
    bool is_empty() const { return m_indices.is_empty(); }
    bool contains(const ModelIndex& index) const { return m_indices.contains(PersistentModelIndex(index)); }
    bool contains_row(int row) const
    {
        for (auto& index : m_indices) {
            if (index.is_valid() && index.row() == row)
                return true;
        }
        return false;
    }

    void set(const ModelIndex&);
    void add(const ModelIndex&);
    void add_all(const Vector<ModelIndex>&);
    void toggle(const ModelIndex&);
    bool remove(const ModelIndex&);
    void clear();

    template<typename Callback>
    void for_each_index(Callback callback)
    {
        m_indices.remove_all_matching([&](PersistentModelIndex& index) -> bool {
            if (!index.is_valid())
                return true;

            callback(index);
            return false;
        });
    }

    template<typename Callback>
    void for_each_index(Callback callback) const
    {
        m_indices.remove_all_matching([&](PersistentModelIndex& index) -> bool {
            if (!index.is_valid())
                return true;

            callback(index);
            return false;
        });
    }

    Vector<ModelIndex> indices() const
    {
        Vector<ModelIndex> selected_indices;

        m_indices.remove_all_matching([&](PersistentModelIndex& index) -> bool {
            if (!index.is_valid())
                return true;

            selected_indices.append(index);
            return false;
        });

        return selected_indices;
    }

    // FIXME: This doesn't guarantee that what you get is the lowest or "first" index selected..
    ModelIndex first() const
    {
        if (m_indices.is_empty())
            return {};
        return *m_indices.begin();
    }

    void remove_matching(Function<bool(const ModelIndex&)>);

private:
    void notify_selection_changed();

    AbstractView& m_view;
    mutable HashTable<PersistentModelIndex> m_indices;
    bool m_disable_notify { false };
    bool m_notify_pending { false };
};

}
