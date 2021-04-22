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

    int size() const { return m_indexes.size(); }
    bool is_empty() const { return m_indexes.is_empty(); }
    bool contains(const ModelIndex& index) const { return m_indexes.contains(index); }
    bool contains_row(int row) const
    {
        for (auto& index : m_indexes) {
            if (index.row() == row)
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
        for (auto& index : indexes())
            callback(index);
    }

    template<typename Callback>
    void for_each_index(Callback callback) const
    {
        for (auto& index : indexes())
            callback(index);
    }

    Vector<ModelIndex> indexes() const
    {
        Vector<ModelIndex> selected_indexes;

        for (auto& index : m_indexes)
            selected_indexes.append(index);

        return selected_indexes;
    }

    // FIXME: This doesn't guarantee that what you get is the lowest or "first" index selected..
    ModelIndex first() const
    {
        if (m_indexes.is_empty())
            return {};
        return *m_indexes.begin();
    }

    void remove_matching(Function<bool(const ModelIndex&)>);

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
    HashTable<ModelIndex> m_indexes;
    bool m_disable_notify { false };
    bool m_notify_pending { false };
};

}
