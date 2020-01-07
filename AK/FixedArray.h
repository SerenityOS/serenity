#pragma once

#include <AK/Vector.h>

namespace AK {

template<typename T>
class FixedArray {
public:
    FixedArray() {}
    explicit FixedArray(size_t size)
        : m_size(size)
    {
        m_elements = (T*)kmalloc(sizeof(T) * m_size);
        for (size_t i = 0; i < m_size; ++i)
            new (&m_elements[i]) T();
    }
    ~FixedArray()
    {
        clear();
    }

    FixedArray(const FixedArray& other)
        : m_size(other.m_size)
    {
        m_elements = (T*)kmalloc(sizeof(T) * m_size);
        for (size_t i = 0; i < m_size; ++i)
            new (&m_elements[i]) T(other[i]);
    }

    FixedArray& operator=(const FixedArray&) = delete;
    FixedArray(FixedArray&&) = delete;
    FixedArray& operator=(FixedArray&&) = delete;

    void clear()
    {
        if (!m_elements)
            return;
        for (size_t i = 0; i < m_size; ++i)
            m_elements[i].~T();
        kfree(m_elements);
        m_elements = nullptr;
        m_size = 0;
    }

    size_t size() const { return m_size; }

    T& operator[](size_t index)
    {
        ASSERT(index < m_size);
        return m_elements[index];
    }

    const T& operator[](size_t index) const
    {
        ASSERT(index < m_size);
        return m_elements[index];
    }

    void resize(size_t new_size)
    {
        if (new_size == m_size)
            return;
        auto* new_elements = (T*)kmalloc(new_size * sizeof(T));
        for (size_t i = 0; i < min(new_size, m_size); ++i)
            new (&new_elements[i]) T(move(m_elements[i]));
        for (size_t i = min(new_size, m_size); i < new_size; ++i)
            new (&new_elements[i]) T();
        for (size_t i = 0; i < m_size; ++i)
            m_elements[i].~T();
        if (m_elements)
            kfree(m_elements);
        m_elements = new_elements;
        m_size = new_size;
    }

    using Iterator = VectorIterator<FixedArray, T>;
    Iterator begin() { return Iterator(*this, 0); }
    Iterator end() { return Iterator(*this, size()); }

    using ConstIterator = VectorIterator<const FixedArray, const T>;
    ConstIterator begin() const { return ConstIterator(*this, 0); }
    ConstIterator end() const { return ConstIterator(*this, size()); }

private:
    size_t m_size { 0 };
    T* m_elements { nullptr };
};

}

using AK::FixedArray;
