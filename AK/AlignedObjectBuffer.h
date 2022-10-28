/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace AK {

template<class T>
class AlignedObjectBuffer {
    alignas(T) u8 m_storage[sizeof(T)];

public:
    [[nodiscard]] T* ptr() noexcept
    {
        return reinterpret_cast<T*>(static_cast<void*>(m_storage));
    }

    [[nodiscard]] u8* buffer() noexcept
    {
        return m_storage;
    }

    [[nodiscard]] T& object() noexcept
    {
        return *reinterpret_cast<T*>(this);
    }

    [[nodiscard]] T const* ptr() const noexcept
    {
        return reinterpret_cast<T const*>(static_cast<void const*>(m_storage));
    }

    [[nodiscard]] u8 const* buffer() const noexcept
    {
        return m_storage;
    }

    [[nodiscard]] T const& object() const noexcept
    {
        return *reinterpret_cast<T const*>(this);
    }

    [[nodiscard]] constexpr size_t byte_size() noexcept
    {
        return sizeof(m_storage);
    }
};

template<class T, size_t Capacity>
class AlignedObjectArrayBuffer {
    static constexpr size_t storage_size()
    {
        if constexpr (Capacity == 0)
            return 0;
        else
            return sizeof(T) * Capacity;
    }

    static constexpr size_t storage_alignment()
    {
        if constexpr (Capacity == 0)
            return 1;
        else
            return alignof(T);
    }

    alignas(storage_alignment()) u8 m_storage[storage_size()];

public:
    [[nodiscard]] T* ptr(size_t index) noexcept
    {
        static_assert(Capacity > 0, "Invalid capacity");
        return reinterpret_cast<T*>(static_cast<void*>(m_storage)) + index;
    }

    [[nodiscard]] T* item_ptr(size_t index) noexcept
    {
        static_assert(Capacity > 0, "Invalid capacity");
        return reinterpret_cast<T*>(static_cast<void*>(m_storage)) + index;
    }

    [[nodiscard]] u8* buffer() noexcept
    {
        static_assert(Capacity > 0, "Invalid capacity");
        return m_storage;
    }

    [[nodiscard]] T& item(size_t index) noexcept
    {
        return *item_ptr(index);
    }

    [[nodiscard]] T const* item_ptr(size_t index) const noexcept
    {
        static_assert(Capacity > 0, "Invalid capacity");
        return reinterpret_cast<T const*>(static_cast<void const*>(m_storage)) + index;
    }

    [[nodiscard]] u8 const* buffer() const noexcept
    {
        static_assert(Capacity > 0, "Invalid capacity");
        return m_storage;
    }

    [[nodiscard]] T const& object(size_t index) const noexcept
    {
        return *item_ptr(index);
    }

    [[nodiscard]] constexpr size_t byte_size() noexcept
    {
        return sizeof(m_storage);
    }
};

}

using AK::AlignedObjectArrayBuffer;
using AK::AlignedObjectBuffer;
