/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Find.h>
#include <AK/StdLibExtras.h>
#include <AK/Traits.h>
#include <AK/Types.h>

namespace AK {

template<typename ListType, typename ElementType>
class SinglyLinkedListIterator {
public:
    SinglyLinkedListIterator() = default;
    bool operator!=(const SinglyLinkedListIterator& other) const { return m_node != other.m_node; }
    SinglyLinkedListIterator& operator++()
    {
        if (m_removed)
            m_removed = false;
        else
            m_prev = m_node;
        m_node = m_next;
        if (m_next)
            m_next = m_next->next;
        return *this;
    }
    ElementType& operator*()
    {
        VERIFY(!m_removed);
        return m_node->value;
    }
    ElementType* operator->()
    {
        VERIFY(!m_removed);
        return &m_node->value;
    }
    bool is_end() const { return !m_node; }
    bool is_begin() const { return !m_prev; }
    void remove(ListType& list)
    {
        m_removed = true;
        list.remove(*this);
    };

private:
    friend ListType;
    explicit SinglyLinkedListIterator(typename ListType::Node* node, typename ListType::Node* prev = nullptr)
        : m_node(node)
        , m_prev(prev)
        , m_next(node ? node->next : nullptr)
    {
    }
    typename ListType::Node* m_node { nullptr };
    typename ListType::Node* m_prev { nullptr };
    typename ListType::Node* m_next { nullptr };
    bool m_removed { false };
};

template<typename T>
class SinglyLinkedList {
private:
    struct Node {
        explicit Node(T&& v)
            : value(move(v))
        {
        }
        explicit Node(const T& v)
            : value(v)
        {
        }
        T value;
        Node* next { nullptr };
    };

public:
    SinglyLinkedList() = default;
    SinglyLinkedList(const SinglyLinkedList& other) = delete;
    SinglyLinkedList(SinglyLinkedList&& other)
        : m_head(other.m_head)
        , m_tail(other.m_tail)
    {
        other.m_head = nullptr;
        other.m_tail = nullptr;
    }
    SinglyLinkedList& operator=(const SinglyLinkedList& other) = delete;
    SinglyLinkedList& operator=(SinglyLinkedList&&) = delete;

    ~SinglyLinkedList() { clear(); }

    bool is_empty() const { return !head(); }

    inline size_t size_slow() const
    {
        size_t size = 0;
        for (auto* node = m_head; node; node = node->next)
            ++size;
        return size;
    }

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
        VERIFY(head());
        return head()->value;
    }
    const T& first() const
    {
        VERIFY(head());
        return head()->value;
    }
    T& last()
    {
        VERIFY(head());
        return tail()->value;
    }
    const T& last() const
    {
        VERIFY(head());
        return tail()->value;
    }

    T take_first()
    {
        VERIFY(m_head);
        auto* prev_head = m_head;
        T value = move(first());
        if (m_tail == m_head)
            m_tail = nullptr;
        m_head = m_head->next;
        delete prev_head;
        return value;
    }

    template<typename U = T>
    void append(U&& value)
    {
        auto* node = new Node(forward<U>(value));
        if (!m_head) {
            m_head = node;
            m_tail = node;
            return;
        }
        m_tail->next = node;
        m_tail = node;
    }

    bool contains_slow(const T& value) const
    {
        return find(value) != end();
    }

    using Iterator = SinglyLinkedListIterator<SinglyLinkedList, T>;
    friend Iterator;
    Iterator begin() { return Iterator(m_head); }
    Iterator end() { return {}; }

    using ConstIterator = SinglyLinkedListIterator<const SinglyLinkedList, const T>;
    friend ConstIterator;
    ConstIterator begin() const { return ConstIterator(m_head); }
    ConstIterator end() const { return {}; }

    template<typename TUnaryPredicate>
    ConstIterator find_if(TUnaryPredicate&& pred) const
    {
        return AK::find_if(begin(), end(), forward<TUnaryPredicate>(pred));
    }

    template<typename TUnaryPredicate>
    Iterator find_if(TUnaryPredicate&& pred)
    {
        return AK::find_if(begin(), end(), forward<TUnaryPredicate>(pred));
    }

    ConstIterator find(const T& value) const
    {
        return find_if([&](auto& other) { return Traits<T>::equals(value, other); });
    }

    Iterator find(const T& value)
    {
        return find_if([&](auto& other) { return Traits<T>::equals(value, other); });
    }

    template<typename U = T>
    void insert_before(Iterator iterator, U&& value)
    {
        auto* node = new Node(forward<U>(value));
        node->next = iterator.m_node;
        if (m_head == iterator.m_node)
            m_head = node;
        if (iterator.m_prev)
            iterator.m_prev->next = node;
    }

    template<typename U = T>
    void insert_after(Iterator iterator, U&& value)
    {
        if (iterator.is_end()) {
            append(value);
            return;
        }

        auto* node = new Node(forward<U>(value));
        node->next = iterator.m_node->next;

        iterator.m_node->next = node;

        if (m_tail == iterator.m_node)
            m_tail = node;
    }

    void remove(Iterator& iterator)
    {
        VERIFY(!iterator.is_end());
        if (m_head == iterator.m_node)
            m_head = iterator.m_node->next;
        if (m_tail == iterator.m_node)
            m_tail = iterator.m_prev;
        if (iterator.m_prev)
            iterator.m_prev->next = iterator.m_node->next;
        delete iterator.m_node;
    }

private:
    Node* head() { return m_head; }
    const Node* head() const { return m_head; }

    Node* tail() { return m_tail; }
    const Node* tail() const { return m_tail; }

    Node* m_head { nullptr };
    Node* m_tail { nullptr };
};

}

using AK::SinglyLinkedList;
