/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>

namespace AK {

template<typename T>
class Ptr32 {
public:
    Ptr32() = default;
    Ptr32(T* ptr)
        : m_ptr((u32) reinterpret_cast<uintptr_t>(ptr))
    {
        VERIFY((reinterpret_cast<uintptr_t>(ptr) & 0xFFFFFFFF) == m_ptr);
    }
    // inline T* operator*() { return (T*)m_ptr; }
    // inline const T* operator*() const { return (T*)m_ptr; }

    inline T* operator->()
    {
        VERIFY(m_ptr);
        return m_ptr;
    }

    inline operator T*() { return (T*)static_cast<uintptr_t>(m_ptr); }
    inline operator const T*() const { return (T*)static_cast<uintptr_t>(m_ptr); }

private:
    u32 m_ptr { 0 };
};

}

using AK::Ptr32;
