/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/HashMap.h>
#include <AK/RedBlackTree.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Map : public Object {
    JS_OBJECT(Map, Object);

public:
    static Map* create(GlobalObject&);

    explicit Map(Object& prototype);
    virtual ~Map() override;

    void map_clear();
    bool map_remove(Value const&);
    Optional<Value> map_get(Value const&) const;
    bool map_has(Value const&) const;
    void map_set(Value const&, Value);
    size_t map_size() const;

    struct EndIterator {
    };

    template<bool IsConst>
    struct IteratorImpl {
        bool is_end() const
        {
            if (m_index.has_value()) {
                return m_map.m_keys.begin_from(*m_index).is_end()
                    && m_map.m_keys.find_smallest_not_below_iterator(*m_index).is_end();
            }

            // First attempt and no iteration, ask the map if it has anything.
            return m_map.m_keys.is_empty();
        }

        IteratorImpl& operator++()
        {
            if (auto it = m_map.m_keys.find_smallest_not_below_iterator(ensure_index() + 1); it.is_end())
                m_index = m_map.m_next_insertion_id;
            else
                m_index = it.key();
            return *this;
        }

        decltype(auto) operator*()
        {
            return *m_map.m_entries.find(*m_map.m_keys.begin_from(ensure_index()));
        }

        decltype(auto) operator*() const
        {
            return *m_map.m_entries.find(*m_map.m_keys.begin_from(ensure_index()));
        }

        bool operator==(IteratorImpl const& other) const { return m_index == other.m_index && &m_map == &other.m_map; }
        bool operator==(EndIterator const&) const { return is_end(); }

    private:
        friend class Map;
        IteratorImpl(Map const& map) requires(IsConst)
            : m_map(map)
        {
        }

        IteratorImpl(Map& map) requires(!IsConst)
            : m_map(map)
        {
        }

        size_t ensure_index()
        {
            if (!m_index.has_value()) {
                VERIFY(!m_map.m_keys.is_empty());
                m_index = m_map.m_keys.begin().key();
            }
            return *m_index;
        }

        Conditional<IsConst, Map const&, Map&> m_map;
        mutable Optional<size_t> m_index;
    };

    using Iterator = IteratorImpl<false>;
    using ConstIterator = IteratorImpl<true>;

    ConstIterator begin() const { return { *this }; }
    Iterator begin() { return { *this }; }
    EndIterator end() const { return {}; }

private:
    virtual void visit_edges(Visitor& visitor) override;

    size_t m_next_insertion_id { 0 };
    RedBlackTree<size_t, Value> m_keys;
    HashMap<Value, Value, ValueTraits> m_entries;
};

}
