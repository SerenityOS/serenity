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
    bool contains(ModelIndex const& index) const { return m_indices.contains(index); }
    bool contains_row(int row) const
    {
        for (auto& index : m_indices) {
            if (index.row() == row)
                return true;
        }
        return false;
    }

    void set(ModelIndex const&);
    void add(ModelIndex const&);
    void add_all(Vector<ModelIndex> const&);
    void toggle(ModelIndex const&);
    bool remove(ModelIndex const&);
    void clear();

    template<typename Callback>
    void for_each_index(Callback callback)
    {
        for (auto& index : indices())
            callback(index);
    }

    template<typename Callback>
    void for_each_index(Callback callback) const
    {
        for (auto& index : indices())
            callback(index);
    }

    Vector<ModelIndex> indices() const
    {
        Vector<ModelIndex> selected_indices;

        for (auto& index : m_indices)
            selected_indices.append(index);

        return selected_indices;
    }

    // FIXME: This doesn't guarantee that what you get is the lowest or "first" index selected..
    ModelIndex first() const
    {
        if (m_indices.is_empty())
            return {};
        return *m_indices.begin();
    }

    void remove_all_matching(Function<bool(ModelIndex const&)> const& filter);

    template<typename Function>
    void change_from_model(Badge<SortingProxyModel>, Function f)
    {
        {
            TemporaryChange change(m_disable_notify, true);
            m_notify_pending = false;
            f(*this);
        }
        if (m_notify_pending)
            notify_selection_changed();
    }

private:
    void notify_selection_changed();

    AbstractView& m_view;
    HashTable<ModelIndex> m_indices;
    bool m_disable_notify { false };
    bool m_notify_pending { false };
};

}
