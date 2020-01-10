#pragma once

#include <AK/HashTable.h>
#include <LibGUI/GModelIndex.h>

class GAbstractView;

class GModelSelection {
public:
    GModelSelection(GAbstractView& view)
        : m_view(view)
    {
    }

    int size() const { return m_indexes.size(); }
    bool is_empty() const { return m_indexes.is_empty(); }
    bool contains(const GModelIndex& index) const { return m_indexes.contains(index); }
    bool contains_row(int row) const
    {
        for (auto& index : m_indexes) {
            if (index.row() == row)
                return true;
        }
        return false;
    }

    void set(const GModelIndex&);
    void add(const GModelIndex&);
    void toggle(const GModelIndex&);
    bool remove(const GModelIndex&);
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

    Vector<GModelIndex> indexes() const
    {
        Vector<GModelIndex> selected_indexes;

        for (auto& index : m_indexes)
            selected_indexes.append(index);

        return selected_indexes;
    }

    // FIXME: This doesn't guarantee that what you get is the lowest or "first" index selected..
    GModelIndex first() const
    {
        if (m_indexes.is_empty())
            return {};
        return *m_indexes.begin();
    }

private:
    GAbstractView& m_view;
    HashTable<GModelIndex> m_indexes;
};
