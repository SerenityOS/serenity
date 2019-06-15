#pragma once

#include "StdLibExtras.h"

namespace AK {

template<typename T>
class SinglyLinkedList {
private:
    struct Node {
        explicit Node(T&& v)
            : value(move(v))
        {
        }
        T value;
        Node* next { nullptr };
    };

public:
    SinglyLinkedList() {}
    ~SinglyLinkedList() { clear(); }

    bool is_empty() const { return !head(); }

    inline int size_slow() const
    {
        int size = 0;
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
        ASSERT(head());
        return head()->value;
    }
    const T& first() const
    {
        ASSERT(head());
        return head()->value;
    }
    T& last()
    {
        ASSERT(head());
        return tail()->value;
    }
    const T& last() const
    {
        ASSERT(head());
        return tail()->value;
    }

    T take_first()
    {
        ASSERT(m_head);
        auto* prev_head = m_head;
        T value = move(first());
        if (m_tail == m_head)
            m_tail = nullptr;
        m_head = m_head->next;
        delete prev_head;
        return value;
    }

    void append(T&& value)
    {
        auto* node = new Node(move(value));
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
        for (auto* node = m_head; node; node = node->next) {
            if (node->value == value)
                return true;
        }
        return false;
    }

    class Iterator {
    public:
        bool operator!=(const Iterator& other) { return m_node != other.m_node; }
        Iterator& operator++()
        {
            m_node = m_node->next;
            return *this;
        }
        T& operator*() { return m_node->value; }
        bool is_end() const { return !m_node; }
        static Iterator universal_end() { return Iterator(nullptr); }

    private:
        friend class SinglyLinkedList;
        explicit Iterator(SinglyLinkedList::Node* node)
            : m_node(node)
        {
        }
        SinglyLinkedList::Node* m_node;
    };

    Iterator begin() { return Iterator(m_head); }
    Iterator end() { return Iterator::universal_end(); }

    class ConstIterator {
    public:
        bool operator!=(const ConstIterator& other) { return m_node != other.m_node; }
        ConstIterator& operator++()
        {
            m_node = m_node->next;
            return *this;
        }
        const T& operator*() const { return m_node->value; }
        bool is_end() const { return !m_node; }
        static ConstIterator universal_end() { return ConstIterator(nullptr); }

    private:
        friend class SinglyLinkedList;
        explicit ConstIterator(const SinglyLinkedList::Node* node)
            : m_node(node)
        {
        }
        const SinglyLinkedList::Node* m_node;
    };

    ConstIterator begin() const { return ConstIterator(m_head); }
    ConstIterator end() const { return ConstIterator::universal_end(); }

    ConstIterator find(const T& value) const
    {
        for (auto* node = m_head; node; node = node->next) {
            if (node->value == value)
                return ConstIterator(node);
        }
        return end();
    }

    Iterator find(const T& value)
    {
        for (auto* node = m_head; node; node = node->next) {
            if (node->value == value)
                return Iterator(node);
        }
        return end();
    }

private:
    friend class Iterator;

    Node* head() { return m_head; }
    const Node* head() const { return m_head; }

    Node* tail() { return m_tail; }
    const Node* tail() const { return m_tail; }

    Node* m_head { nullptr };
    Node* m_tail { nullptr };
};

}

using AK::SinglyLinkedList;
