#pragma once

// KBuffer: Statically sized kernel-only memory buffer.
//
// A KBuffer is a value-type convenience class that wraps a NonnullRefPtr<KBufferImpl>.
// The memory is allocated via the global kernel-only page allocator, rather than via
// kmalloc() which is what ByteBuffer/Vector/etc will use.
//
// This makes KBuffer a little heavier to allocate, but much better for large and/or
// long-lived allocations, since they don't put all that weight and pressure on the
// severely limited kmalloc heap.

#include <AK/Assertions.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Region.h>

class KBufferImpl : public RefCounted<KBufferImpl> {
public:
    static NonnullRefPtr<KBufferImpl> create_with_size(size_t size)
    {
        auto region = MM.allocate_kernel_region(PAGE_ROUND_UP(size), "KBuffer");
        ASSERT(region);
        return adopt(*new KBufferImpl(*region, size));
    }

    static NonnullRefPtr<KBufferImpl> copy(const void* data, size_t size)
    {
        auto buffer = create_with_size(size);
        memcpy(buffer->data(), data, size);
        return buffer;
    }

    u8* data() { return m_region->vaddr().as_ptr(); }
    const u8* data() const { return m_region->vaddr().as_ptr(); }
    size_t size() const { return m_size; }
    size_t capacity() const { return m_region->size(); }

private:
    explicit KBufferImpl(NonnullRefPtr<Region>&& region, size_t size)
        : m_size(size)
        , m_region(move(region))
    {
    }

    size_t m_size { 0 };
    NonnullRefPtr<Region> m_region;
};

class KBuffer {
public:
    static KBuffer create_with_size(size_t size)
    {
        return KBuffer(KBufferImpl::create_with_size(size));
    }

    static KBuffer copy(const void* data, size_t size)
    {
        return KBuffer(KBufferImpl::copy(data, size));
    }

    u8* data() { return m_impl->data(); }
    const u8* data() const { return m_impl->data(); }
    size_t size() const { return m_impl->size(); }
    size_t capacity() const { return m_impl->size(); }

    const KBufferImpl& impl() const { return m_impl; }

    KBuffer(NonnullRefPtr<KBufferImpl>&& impl)
        : m_impl(move(impl))
    {
    }

private:
    NonnullRefPtr<KBufferImpl> m_impl;
};
