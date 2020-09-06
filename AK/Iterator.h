/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

    constexpr const ValueType& operator*() const { return m_container[m_index]; }
    constexpr ValueType& operator*() { return m_container[m_index]; }

    constexpr const ValueType* operator->() const { return &m_container[m_index]; }
    constexpr ValueType* operator->() { return &m_container[m_index]; }

private:
    static constexpr SimpleIterator begin(Container& container) { return { container, 0 }; }
    static constexpr SimpleIterator end(Container& container)
    {
        using RawContainerType = typename RemoveCV<Container>::Type;

        if constexpr (IsSame<StringView, RawContainerType>::value || IsSame<String, RawContainerType>::value)
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
