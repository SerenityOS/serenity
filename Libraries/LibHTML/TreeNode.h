#pragma once

#include <AK/Assertions.h>
#include <AK/NonnullRefPtr.h>

template<typename T>
class TreeNode {
public:
    void ref()
    {
        ASSERT(m_ref_count);
        ++m_ref_count;
    }

    void deref()
    {
        ASSERT(m_ref_count);
        if (!--m_ref_count)
            delete static_cast<T*>(this);
    }
    int ref_count() const { return m_ref_count; }

    T* parent() { return m_parent; }
    const T* parent() const { return m_parent; }

    bool has_children() const { return m_first_child; }
    T* next_sibling() { return m_next_sibling; }
    T* previous_sibling() { return m_previous_sibling; }
    T* first_child() { return m_first_child; }
    T* last_child() { return m_last_child; }
    const T* next_sibling() const { return m_next_sibling; }
    const T* previous_sibling() const { return m_previous_sibling; }
    const T* first_child() const { return m_first_child; }
    const T* last_child() const { return m_last_child; }

    void append_child(NonnullRefPtr<T> node);

protected:
    TreeNode() { }

private:
    int m_ref_count { 1 };
    T* m_parent { nullptr };
    T* m_first_child { nullptr };
    T* m_last_child { nullptr };
    T* m_next_sibling { nullptr };
    T* m_previous_sibling { nullptr };
};

template<typename T>
inline void TreeNode<T>::append_child(NonnullRefPtr<T> node)
{
    ASSERT(!node->m_parent);
    if (m_last_child)
        m_last_child->m_next_sibling = node.ptr();
    node->m_previous_sibling = m_last_child;
    node->m_parent = static_cast<T*>(this);
    m_last_child = &node.leak_ref();
    if (!m_first_child)
        m_first_child = m_last_child;
}
