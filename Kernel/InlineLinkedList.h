#pragma once

#include "Assertions.h"
#include "types.h"

template<typename T> class InlineLinkedListNode {
public:
    InlineLinkedListNode();
    
    void setPrev(T*);
    void setNext(T*);
    
    T* prev() const;
    T* next() const;
};

template<typename T> inline InlineLinkedListNode<T>::InlineLinkedListNode()
{
    setPrev(0);
    setNext(0);
}

template<typename T> inline void InlineLinkedListNode<T>::setPrev(T* prev)
{
    static_cast<T*>(this)->m_prev = prev;
}

template<typename T> inline void InlineLinkedListNode<T>::setNext(T* next)
{
    static_cast<T*>(this)->m_next = next;
}

template<typename T> inline T* InlineLinkedListNode<T>::prev() const
{
    return static_cast<const T*>(this)->m_prev;
}

template<typename T> inline T* InlineLinkedListNode<T>::next() const
{
    return static_cast<const T*>(this)->m_next;
}

template<typename T> class InlineLinkedList {
public:
    InlineLinkedList() { }
    
    bool isEmpty() const { return !m_head; }
    size_t sizeSlow() const;
    void clear();

    T* head() const { return m_head; }
    T* removeHead();

    T* tail() const { return m_tail; }

    void prepend(T*);
    void append(T*);
    void remove(T*);
    void append(InlineLinkedList<T>&);

    bool containsSlow(T* value) const
    {
        for (T* node = m_head; node; node = node->next()) {
            if (node == value)
                return true;
        }
        return false;
    }

private:
    T* m_head { nullptr };
    T* m_tail { nullptr };
};

template<typename T> inline size_t InlineLinkedList<T>::sizeSlow() const
{
    size_t size = 0;
    for (T* node = m_head; node; node = node->next())
        ++size;
    return size;
}

template<typename T> inline void InlineLinkedList<T>::clear()
{
    m_head = 0;
    m_tail = 0;
}

template<typename T> inline void InlineLinkedList<T>::prepend(T* node)
{
    if (!m_head) {
        ASSERT(!m_tail);
        m_head = node;
        m_tail = node;
        node->setPrev(0);
        node->setNext(0);
        return;
    }

    ASSERT(m_tail);
    m_head->setPrev(node);
    node->setNext(m_head);
    node->setPrev(0);
    m_head = node;
}

template<typename T> inline void InlineLinkedList<T>::append(T* node)
{
    if (!m_tail) {
        ASSERT(!m_head);
        m_head = node;
        m_tail = node;
        node->setPrev(0);
        node->setNext(0);
        return;
    }

    ASSERT(m_head);
    m_tail->setNext(node);
    node->setPrev(m_tail);
    node->setNext(0);
    m_tail = node;
}

template<typename T> inline void InlineLinkedList<T>::remove(T* node)
{
    if (node->prev()) {
        ASSERT(node != m_head);
        node->prev()->setNext(node->next());
    } else {
        ASSERT(node == m_head);
        m_head = node->next();
    }

    if (node->next()) {
        ASSERT(node != m_tail);
        node->next()->setPrev(node->prev());
    } else {
        ASSERT(node == m_tail);
        m_tail = node->prev();
    }
}

template<typename T> inline T* InlineLinkedList<T>::removeHead()
{
    T* node = head();
    if (node)
        remove(node);
    return node;
}

template<typename T> inline void InlineLinkedList<T>::append(InlineLinkedList<T>& other)
{
    if (!other.head())
        return;

    if (!head()) {
        m_head = other.head();
        m_tail = other.tail();
        other.clear();
        return;
    }

    ASSERT(tail());
    ASSERT(other.head());
    T* otherHead = other.head();
    T* otherTail = other.tail();
    other.clear();

    ASSERT(!m_tail->next());
    m_tail->setNext(otherHead);
    ASSERT(!otherHead->prev());
    otherHead->setPrev(m_tail);
    m_tail = otherTail;
}
