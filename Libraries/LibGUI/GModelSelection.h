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

    bool is_empty() const { return m_indexes.is_empty(); }
    bool contains(const GModelIndex& index) const { return m_indexes.contains(index); }

    void set(const GModelIndex&);
    void add(const GModelIndex&);
    bool remove(const GModelIndex&);
    void clear();

    template<typename Callback>
    void for_each_index(Callback callback)
    {
        for (auto& index : m_indexes)
            callback(index);
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
