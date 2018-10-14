#pragma once

#include <utility>

namespace AK {

template<typename T>
class DoublyLinkedList {
private:
    struct Node {
        explicit Node(T&& v) : value(v) { }
        T value;
        Node* next { nullptr };
        Node* prev { nullptr };
    };

public:
    DoublyLinkedList() { }
    ~DoublyLinkedList() { clear(); }

    bool isEmpty() const { return !head(); }

    void clear()
    {
        for (auto* node = m_head; node; ) {
            auto* next = node->next;
            delete node;
            node = next;
        }
        m_head = nullptr;
        m_tail = nullptr;
    }

    T& first() { ASSERT(head()); return head()->value; }
    const T& first() const { ASSERT(head()); return head()->value; }
    T& last() { ASSERT(head()); return tail()->value; }
    const T& last() const { ASSERT(head()); return tail()->value; }

    void append(T&& value)
    {
        auto* node = new Node(std::move(value));
        if (!m_head) {
            ASSERT(!m_tail);
            m_head = node;
            m_tail = node;
            return;
        }
        ASSERT(m_tail);
        m_tail->next = node;
        node->prev = m_tail;
        m_tail = node;
    }

    bool containsSlow(const T& value) const
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
        Iterator& operator++() { m_node = m_node->next; return *this; }
        T& operator*() { return m_node->value; }
        bool isEnd() const { return !m_node; }
        static Iterator universalEnd() { return Iterator(nullptr); }
    private:
        friend class DoublyLinkedList;
        explicit Iterator(DoublyLinkedList::Node* node) : m_node(node) { }
        DoublyLinkedList::Node* m_node;
    };

    Iterator begin() { return Iterator(m_head); }
    Iterator end() { return Iterator::universalEnd(); }

    class ConstIterator {
    public:
        bool operator!=(const ConstIterator& other) { return m_node != other.m_node; }
        ConstIterator& operator++() { m_node = m_node->next; return *this; }
        const T& operator*() const { return m_node->value; }
        bool isEnd() const { return !m_node; }
        static ConstIterator universalEnd() { return ConstIterator(nullptr); }
    private:
        friend class DoublyLinkedList;
        explicit ConstIterator(const DoublyLinkedList::Node* node) : m_node(node) { }
        const DoublyLinkedList::Node* m_node;
    };

    ConstIterator begin() const { return ConstIterator(m_head); }
    ConstIterator end() const { return ConstIterator::universalEnd(); }

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

    void remove(Iterator& it)
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
    friend class Iterator;

    Node* head() { return m_head; }
    const Node* head() const { return m_head; }

    Node* tail() { return m_tail; }
    const Node* tail() const { return m_tail; }

    Node* m_head { nullptr };
    Node* m_tail { nullptr };
};

}

using AK::DoublyLinkedList;

