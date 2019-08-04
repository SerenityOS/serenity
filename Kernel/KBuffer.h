#pragma once

#include <AK/Assertions.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Region.h>

class KBuffer : public RefCounted<KBuffer> {
public:
    static NonnullRefPtr<KBuffer> create_with_size(size_t size)
    {
        auto region = MM.allocate_kernel_region(PAGE_ROUND_UP(size), "KBuffer");
        ASSERT(region);
        return adopt(*new KBuffer(*region, size));
    }

    static NonnullRefPtr<KBuffer> copy(const void* data, size_t size)
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
    explicit KBuffer(NonnullRefPtr<Region>&& region, size_t size)
        : m_size(size)
        , m_region(move(region))
    {
    }

    size_t m_size { 0 };
    NonnullRefPtr<Region> m_region;
};
