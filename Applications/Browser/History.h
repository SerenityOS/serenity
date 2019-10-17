#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>

template<typename T>
class History final {
public:
    void push(const T& item);
    T current() const;

    void go_back();
    void go_forward();

    bool can_go_back() { return m_current > 0; }
    bool can_go_forward() { return m_current + 1 < m_items.size(); }

    void clear();

private:
    Vector<T> m_items;
    int m_current { -1 };
};

template<typename T>
inline void History<T>::push(const T& item)
{
    m_items.shrink(m_current + 1);
    m_items.append(item);
    m_current++;
}

template<typename T>
inline T History<T>::current() const
{
    if (m_current == -1)
        return {};
    return m_items[m_current];
}

template<typename T>
inline void History<T>::go_back()
{
    ASSERT(can_go_back());
    m_current--;
}

template<typename T>
inline void History<T>::go_forward()
{
    ASSERT(can_go_forward());
    m_current++;
}

template<typename T>
inline void History<T>::clear()
{
    m_items = {};
    m_current = -1;
}
