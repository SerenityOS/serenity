#pragma once

#include "types.h"

template<typename T>
class Queue {
public:
    struct Node {
        explicit Node(T&& value)
            : value(move(value))
        { }

        Node* next { nullptr };
        Node* prev { nullptr };
        T value;
    };

    Queue() { }
    ~Queue()
    {
        while (!isEmpty())
            dequeue();
    }

    bool isEmpty() const { return !m_head; }
    void enqueue(T&& item)
    {
        auto* newNode = new Node(move(item));
        if (!m_head) {
            m_head = newNode;
            m_tail = newNode;
        } else if (m_tail) {
            newNode->prev = m_tail;
            m_tail->next = newNode;
            m_tail = newNode;
        }
        dump("enqueue");
    }

    T dequeue()
    {
        ASSERT(m_head);
        T value = move(m_head->value);
        auto* oldHead = m_head;
        if (oldHead->next) {
            oldHead->next->prev = nullptr;
            m_head = oldHead->next;
        } else {
            m_head = nullptr;
        }
        if (m_tail == oldHead)
            m_tail = nullptr;
        delete oldHead;
        dump("dequeue");
        //asm volatile("cli;hlt");
        return value;
    }

    Node* head() { return m_head; }

    T take(Node& node)
    {
        T value = move(node.value);
        if (node.prev) {
            node.prev->next = node.next;
        } else {
            m_head = node.next;
        }
        if (node.next) {
            node.next->prev = node.prev;
        } else {
            m_tail = node.prev;
        }
        delete &node;
        dump("take");
        return value;
    }

private:
    void dump(const char* op)
    {
        return;
        asm volatile("cli");
        ASSERT(m_head != (void*)0xaaaaaaaa);
        ASSERT(m_tail != (void*)0xaaaaaaaa);
        kprintf("Queue %p after %s: {m_head=%p, m_tail=%p}\n", this, op, m_head, m_tail);
        for (auto* node = m_head; node; node = node->next) {
            kprintf("  Queue::Node %p%s%s next=%p prev=%p\n", node, node == m_head ? " (head)" : "", node == m_tail ? " (tail)" : "", node->next, node->prev);
        }
        asm volatile("sti");
    }

    Node* m_head { nullptr };
    Node* m_tail { nullptr };
};
