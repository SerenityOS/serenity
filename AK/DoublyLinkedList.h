/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Assertions.h>
#include <AK/Find.h>
#include <AK/StdLibExtras.h>

namespace AK {

template<typename ListType, typename ElementType>
class DoublyLinkedListIterator {
public:
    bool operator!=(const DoublyLinkedListIterator& other) const { return m_node != other.m_node; }
    bool operator==(const DoublyLinkedListIterator& other) const { return m_node == other.m_node; }
    DoublyLinkedListIterator& operator++()
    {
        m_node = m_node->next;
        return *this;
    }
    ElementType& operator*() { return m_node->value; }
    ElementType* operator->() { return &m_node->value; }
    bool is_end() const { return !m_node; }
    static DoublyLinkedListIterator universal_end() { return DoublyLinkedListIterator(nullptr); }

private:
    friend ListType;
    explicit DoublyLinkedListIterator(typename ListType::Node* node)
        : m_node(node)
    {
    }
    typename ListType::Node* m_node;
};

template<typename T>
class DoublyLinkedList {
private:
    struct Node {
        template<typename U>
        explicit Node(U&& v)
            : value(forward<U>(v))
        {
            static_assert(
                requires { T(v); }, "Conversion operator is missing.");
        }
        T value;
        Node* next { nullptr };
        Node* prev { nullptr };
    };

public:
    DoublyLinkedList() = default;
    ~DoublyLinkedList() { clear(); }

    bool is_empty() const { return !m_head; }

    void clear()
    {
        for (auto* node = m_head; node;) {
            auto* next = node->next;
            delete node;
            node = next;
        }
        m_head = nullptr;
        m_tail = nullptr;
    }

    T& first()
    {
        ASSERT(m_head);
        return m_head->value;
    }
    const T& first() const
    {
        ASSERT(m_head);
        return m_head->value;
    }
    T& last()
    {
        ASSERT(m_head);
        return m_tail->value;
    }
    const T& last() const
    {
        ASSERT(m_head);
        return m_tail->value;
    }

    template<typename U>
    void append(U&& value)
    {
        static_assert(
            requires { T(value); }, "Conversion operator is missing.");
        auto* node = new Node(forward<U>(value));
        if (!m_head) {
            ASSERT(!m_tail);
            m_head = node;
            m_tail = node;
            return;
        }
        ASSERT(m_tail);
        ASSERT(!node->next);
        m_tail->next = node;
        node->prev = m_tail;
        m_tail = node;
    }

    template<typename U>
    void prepend(U&& value)
    {
        static_assert(IsSame<T, U>::value);
        auto* node = new Node(forward<U>(value));
        if (!m_head) {
            ASSERT(!m_tail);
            m_head = node;
            m_tail = node;
            return;
        }
        ASSERT(m_tail);
        ASSERT(!node->prev);
        m_head->prev = node;
        node->next = m_head;
        m_head = node;
    }

    bool contains_slow(const T& value) const
    {
        return find(value) != end();
    }

    using Iterator = DoublyLinkedListIterator<DoublyLinkedList, T>;
    friend Iterator;
    Iterator begin() { return Iterator(m_head); }
    Iterator end() { return Iterator::universal_end(); }

    using ConstIterator = DoublyLinkedListIterator<const DoublyLinkedList, const T>;
    friend ConstIterator;
    ConstIterator begin() const { return ConstIterator(m_head); }
    ConstIterator end() const { return ConstIterator::universal_end(); }

    ConstIterator find(const T& value) const
    {
        return AK::find(begin(), end(), value);
    }

    Iterator find(const T& value)
    {
        return AK::find(begin(), end(), value);
    }

    void remove(Iterator it)
    {
        ASSERT(it.m_node);
        auto* node = it.m_node;
        if (node->prev) {
            ASSERT(node != m_head);
            node->prev->next = node->next;
        } else {
            ASSERT(node == m_head);
            m_head = node->next;
        }
        if (node->next) {
            ASSERT(node != m_tail);
            node->next->prev = node->prev;
        } else {
            ASSERT(node == m_tail);
            m_tail = node->prev;
        }
        delete node;
    }

private:
    Node* m_head { nullptr };
    Node* m_tail { nullptr };
};

}

using AK::DoublyLinkedList;
