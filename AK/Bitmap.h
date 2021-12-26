#pragma once

#include "Assertions.h"
#include "StdLibExtras.h"
#include "Types.h"
#include "kmalloc.h"

namespace AK {

class Bitmap {
public:
    // NOTE: A wrapping Bitmap won't try to free the wrapped data.
    static Bitmap wrap(byte* data, int size)
    {
        return Bitmap(data, size);
    }

    static Bitmap create(int size, bool default_value = 0)
    {
        return Bitmap(size, default_value);
    }

    static Bitmap create()
    {
        return Bitmap();
    }

    ~Bitmap()
    {
        if (m_owned)
            kfree(m_data);
        m_data = nullptr;
    }

    int size() const { return m_size; }
    bool get(int index) const
    {
        ASSERT(index < m_size);
        return 0 != (m_data[index / 8] & (1u << (index % 8)));
    }
    void set(int index, bool value) const
    {
        ASSERT(index < m_size);
        if (value)
            m_data[index / 8] |= static_cast<byte>((1u << (index % 8)));
        else
            m_data[index / 8] &= static_cast<byte>(~(1u << (index % 8)));
    }

    byte* data() { return m_data; }
    const byte* data() const { return m_data; }

    void grow(int size, bool default_value)
    {
        ASSERT(size > m_size);

        auto previous_size_bytes = size_in_bytes();
        auto previous_size = m_size;
        auto previous_data = m_data;

        m_size = size;
        m_data = reinterpret_cast<byte*>(kmalloc(size_in_bytes()));

        fill(default_value);

        if (previous_data != nullptr) {
            memcpy(m_data, previous_data, previous_size_bytes);

            if ((previous_size % 8) != 0) {
                if (default_value)
                    m_data[previous_size_bytes - 1] |= (0xff >> (previous_size % 8));
                else
                    m_data[previous_size_bytes - 1] &= ~(0xff >> (previous_size % 8));
            }

            kfree(previous_data);
        }
    }

    void fill(bool value)
    {
        memset(m_data, value ? 0xff : 0x00, size_in_bytes());
    }

    int find_first_set() const
    {
        int i = 0;
        while (i < m_size / 8 && m_data[i] == 0x00)
            i++;

        int j = 0;
        for (j = i * 8; j < m_size; j++)
            if (get(j))
                return j;

        return -1;
    }

    int find_first_unset() const
    {
        int i = 0;
        while (i < m_size / 8 && m_data[i] == 0xff)
            i++;

        int j = 0;
        for (j = i * 8; j < m_size; j++)
            if (!get(j))
                return j;

        return -1;
    }

private:
    explicit Bitmap()
        : m_size(0)
        , m_owned(true)
    {
        m_data = nullptr;
    }

    explicit Bitmap(int size, bool default_value)
        : m_size(size)
        , m_owned(true)
    {
        ASSERT(m_size != 0);
        m_data = reinterpret_cast<byte*>(kmalloc(size_in_bytes()));
        fill(default_value);
    }

    Bitmap(byte* data, int size)
        : m_data(data)
        , m_size(size)
        , m_owned(false)
    {
    }

    int size_in_bytes() const { return ceil_div(m_size, 8); }

    byte* m_data { nullptr };
    int m_size { 0 };
    bool m_owned { false };
};

}

using AK::Bitmap;
