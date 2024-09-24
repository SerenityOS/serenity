/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// KBuffer: Memory buffer backed by a kernel region.
//
// The memory is allocated via the global kernel-only page allocator, rather than via
// kmalloc() which is what ByteBuffer/Vector/etc will use.
//
// This makes KBuffer a little heavier to allocate, but much better for large and/or
// long-lived allocations, since they don't put all that weight and pressure on the
// severely limited kmalloc heap.

#include <AK/Assertions.h>
#include <AK/StringView.h>
#include <Kernel/Library/StdLib.h> // For memcpy. FIXME: Make memcpy less expensive to access a declaration of in the Kernel.
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel {

class [[nodiscard]] KBuffer {
public:
    static ErrorOr<NonnullOwnPtr<KBuffer>> try_create_with_size(StringView name, size_t size, Memory::Region::Access access = Memory::Region::Access::ReadWrite, AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        auto rounded_size = TRY(Memory::page_round_up(size));
        auto region = TRY(MM.allocate_kernel_region(rounded_size, name, access, strategy));
        return TRY(adopt_nonnull_own_or_enomem(new (nothrow) KBuffer { size, move(region) }));
    }

    static ErrorOr<NonnullOwnPtr<KBuffer>> try_create_with_bytes(StringView name, ReadonlyBytes bytes, Memory::Region::Access access = Memory::Region::Access::ReadWrite, AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        auto buffer = TRY(try_create_with_size(name, bytes.size(), access, strategy));
        memcpy(buffer->data(), bytes.data(), bytes.size());
        return buffer;
    }

    UserOrKernelBuffer as_kernel_buffer() { return UserOrKernelBuffer::for_kernel_buffer(data()); }

    [[nodiscard]] u8* data() { return m_region->vaddr().as_ptr(); }
    [[nodiscard]] u8 const* data() const { return m_region->vaddr().as_ptr(); }
    [[nodiscard]] size_t size() const { return m_size; }
    [[nodiscard]] size_t capacity() const { return m_region->size(); }

    [[nodiscard]] ReadonlyBytes bytes() const { return { data(), size() }; }
    [[nodiscard]] Bytes bytes() { return { data(), size() }; }

    void set_size(size_t size)
    {
        VERIFY(size <= capacity());
        m_size = size;
    }

private:
    explicit KBuffer(size_t size, NonnullOwnPtr<Memory::Region> region)
        : m_size(size)
        , m_region(move(region))
    {
    }

    size_t m_size { 0 };
    NonnullOwnPtr<Memory::Region> m_region;
};

}
