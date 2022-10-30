/*
 * Copyright (c) 2022, Kim Nilsson <kim@wayoftao.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace Kernel {

class StackWriter {
public:
    explicit StackWriter(FlatPtr top)
        : m_top { top }
    {
    }

    // Decrement the stack top, reinterpret the intermediate bytes as an (uninitialized) object of type T, and return a reference to it.
    // FIXME: This is probably technically UB, but works by tradition. See C++23 std::start_lifetime_as.
    template<typename T>
    [[nodiscard]] T& emplace(size_t offset = 0)
    {
        // Offsetting only makes sense if it is less than the size
        // of the object we are trying to emplace.
        VERIFY(offset < sizeof(T));

        m_top -= (sizeof(T) - offset);

        // We probably never want to use an unaligned address.
        VERIFY(m_top % alignof(T) == 0);

        return *reinterpret_cast<T*>(m_top);
    }

    // Decrement the stack top and perfect-forward-construct an object of type T on top of the intermediate bytes.
    template<typename T>
    void push(T&& x)
    {
        // T may be a const or ref type.
        using RawT = AK::Detail::Decay<T>;

        m_top -= sizeof(RawT);

        // We probably never want to use an unaligned address.
        VERIFY(m_top % alignof(RawT) == 0);

        new (reinterpret_cast<void*>(m_top)) RawT(forward<T>(x));
    }

    [[nodiscard]] FlatPtr get() const { return m_top; };

private:
    FlatPtr m_top;
};

}
