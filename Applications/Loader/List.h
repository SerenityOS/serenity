#pragma once
#include "Heap.h"

template<typename T>
class List {
public:
    ~List()
    {
        Node* current = m_head;
        while (current) {
            Node* next = current->next;
            free(current);
            current = next;
        }
    }

    struct Node {
        T value;
        Node* next { nullptr };
    };

    class ListIterator {
    public:
        ListIterator() {}
        ListIterator(Node* node)
            : m_node(node)
        {
        }

        ListIterator& operator++()
        {
            m_node = m_node->next;
            return *this;
        }

        bool operator==(const ListIterator& other) const
        {
            return m_node == other.m_node;
        }
        bool operator!=(const ListIterator& other) const
        {
            return !(*this == other);
        }

        T& operator*()
        {
            return m_node->value;
        }

        const T& operator*() const
        {
            return *m_node->value;
        }

    private:
        Node* m_node { nullptr };
    };

    void append(const T& t)
    {
        return append(T(t));
    }

    void append(T&& t)
    {
        if (!m_head) {
            m_head = allocate_node(move(t));
            return;
        }
        Node* current = m_head;
        while (current->next) {
            current = current->next;
        }
        current->next = allocate_node(move(t));
    }

    ListIterator begin()
    {
        return ListIterator(m_head);
    }
    ListIterator end()
    {
        return {};
    }

private:
    Node* allocate_node(T&& t)
    {
        Node* node = (Node*)malloc(sizeof(Node));
        node->value = t;
        return node;
    }

    Node* m_head { nullptr };
};
