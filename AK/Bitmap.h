#pragma once

#include "StdLib.h"
#include "Types.h"
#include "kmalloc.h"

namespace AK {

class Bitmap {
public:
    // NOTE: A wrapping Bitmap won't try to free the wrapped data.
    static Bitmap wrap(byte* data, unsigned size)
    {
        return Bitmap(data, size);
    }

    static Bitmap create(unsigned size)
    {
        return Bitmap(size);
    }

    ~Bitmap()
    {
        if (m_owned)
            kfree(m_data);
        m_data = nullptr;
    }

    unsigned size() const { return m_size; }
    bool get(unsigned index) const
    {
        ASSERT(index < m_size);
        return 0 != (m_data[index / 8] & (1u << (index % 8)));
    }
    void set(unsigned index, bool value) const
    {
        ASSERT(index < m_size);
        if (value)
            m_data[index / 8] |= static_cast<byte>((1u << (index % 8)));
        else
            m_data[index / 8] &= static_cast<byte>(~(1u << (index % 8)));
    }

    byte* data() { return m_data; }
    const byte* data() const { return m_data; }

private:
    explicit Bitmap(unsigned size)
        : m_size(size)
        , m_owned(true)
    {
        ASSERT(m_size != 0);
        m_data = reinterpret_cast<byte*>(kmalloc(ceilDiv(size, 8u)));
    }

    Bitmap(byte* data, unsigned size)
        : m_data(data)
        , m_size(size)
        , m_owned(false)
    {
    }

    byte* m_data { nullptr };
    unsigned m_size { 0 };
    bool m_owned { false };
};

}

using AK::Bitmap;

