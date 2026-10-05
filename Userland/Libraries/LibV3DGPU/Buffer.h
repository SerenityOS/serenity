/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Noncopyable.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace V3DGPU {

class Buffer {
    AK_MAKE_NONCOPYABLE(Buffer);

public:
    ~Buffer();

    Buffer(Buffer&& other)
        : m_gpu_virtual_address(other.m_gpu_virtual_address)
        , m_size(other.m_size)
        , m_mmap_address(other.m_mmap_address)
    {
        other.m_gpu_virtual_address = 0;
        other.m_size = 0;
        other.m_mmap_address = nullptr;
    }

    Buffer& operator=(Buffer&& other)
    {
        if (this != &other) {
            this->~Buffer();
            m_size = exchange(other.m_size, 0);
            m_gpu_virtual_address = exchange(other.m_gpu_virtual_address, 0);
            m_mmap_address = exchange(other.m_mmap_address, nullptr);
        }

        return *this;
    }

    static ErrorOr<Buffer> create(u32 size, StringView name);

    Bytes data() { return Bytes { m_mmap_address, m_size }; }
    ReadonlyBytes data() const { return ReadonlyBytes { m_mmap_address, m_size }; }

    u32 size() const { return m_size; }
    u32 gpu_virtual_address() const { return m_gpu_virtual_address; }

private:
    Buffer(u32 address, u32 size, void* mmap_address)
        : m_gpu_virtual_address(address)
        , m_size(size)
        , m_mmap_address(mmap_address)
    {
    }

    u32 m_gpu_virtual_address { 0 };
    u32 m_size { 0 };

    void* m_mmap_address { nullptr };
};

}
