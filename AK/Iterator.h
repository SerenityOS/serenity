/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace AK {

template<typename Container, typename ValueType>
class SimpleIterator {
public:
    friend Container;

    constexpr bool is_end() const { return m_index == SimpleIterator::end(m_container).m_index; }
    constexpr size_t index() const { return m_index; }

    constexpr bool operator==(SimpleIterator other) const { return m_index == other.m_index; }
    constexpr bool operator!=(SimpleIterator other) const { return m_index != other.m_index; }
    constexpr bool operator<(SimpleIterator other) const { return m_index < other.m_index; }
    constexpr bool operator>(SimpleIterator other) const { return m_index > other.m_index; }
    constexpr bool operator<=(SimpleIterator other) const { return m_index <= other.m_index; }
    constexpr bool operator>=(SimpleIterator other) const { return m_index >= other.m_index; }

    constexpr SimpleIterator operator+(ptrdiff_t delta) const { return SimpleIterator { m_container, m_index + delta }; }
    constexpr SimpleIterator operator-(ptrdiff_t delta) const { return SimpleIterator { m_container, m_index - delta }; }

    constexpr ptrdiff_t operator-(SimpleIterator other) const { return static_cast<ptrdiff_t>(m_index) - other.m_index; }

    constexpr SimpleIterator operator++()
    {
        ++m_index;
        return *this;
    }
    constexpr SimpleIterator operator++(int)
    {
        ++m_index;
        return SimpleIterator { m_container, m_index - 1 };
    }

    constexpr SimpleIterator operator--()
    {
        --m_index;
        return *this;
    }
    constexpr SimpleIterator operator--(int)
    {
        --m_index;
        return SimpleIterator { m_container, m_index + 1 };
    }

    ALWAYS_INLINE constexpr ValueType const& operator*() const { return m_container[m_index]; }
    ALWAYS_INLINE constexpr ValueType& operator*() { return m_container[m_index]; }

    ALWAYS_INLINE constexpr ValueType const* operator->() const { return &m_container[m_index]; }
    ALWAYS_INLINE constexpr ValueType* operator->() { return &m_container[m_index]; }

    SimpleIterator& operator=(SimpleIterator const& other)
    {
        m_index = other.m_index;
        return *this;
    }
    SimpleIterator(SimpleIterator const& obj) = default;

private:
    static constexpr SimpleIterator begin(Container& container) { return { container, 0 }; }
    static constexpr SimpleIterator end(Container& container)
    {
        using RawContainerType = RemoveCV<Container>;

        if constexpr (IsSame<StringView, RawContainerType> || IsSame<ByteString, RawContainerType>)
            return { container, container.length() };
        else
            return { container, container.size() };
    }

    constexpr SimpleIterator(Container& container, size_t index)
        : m_container(container)
        , m_index(index)
    {
    }

    Container& m_container;
    size_t m_index;
};

}
