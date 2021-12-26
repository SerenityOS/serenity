#include <AK/Assertions.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/VM/Region.h>

template<size_t templated_slab_size>
class SlabAllocator {
public:
    SlabAllocator() {}

    void init(size_t size)
    {
        void* base = kmalloc_eternal(size);
        FreeSlab* slabs = (FreeSlab*)base;
        size_t slab_count = size / templated_slab_size;
        for (size_t i = 1; i < slab_count; ++i) {
            slabs[i].next = &slabs[i - 1];
        }
        slabs[0].next = nullptr;
        m_freelist = &slabs[slab_count - 1];
        m_num_allocated = 0;
        m_num_free = slab_count;
    }

    constexpr size_t slab_size() const { return templated_slab_size; }

    void* alloc()
    {
        InterruptDisabler disabler;
        ASSERT(m_freelist);
        void* ptr = m_freelist;
        m_freelist = m_freelist->next;
        ++m_num_allocated;
        --m_num_free;
        return ptr;
    }

    void dealloc(void* ptr)
    {
        InterruptDisabler disabler;
        ASSERT(ptr);
        ((FreeSlab*)ptr)->next = m_freelist;
        m_freelist = (FreeSlab*)ptr;
        ++m_num_allocated;
        --m_num_free;
    }

    size_t num_allocated() const { return m_num_allocated; }
    size_t num_free() const { return m_num_free; }

private:
    struct FreeSlab {
        FreeSlab* next { nullptr };
        char padding[templated_slab_size - sizeof(FreeSlab*)];
    };

    FreeSlab* m_freelist { nullptr };
    size_t m_num_allocated { 0 };
    size_t m_num_free { 0 };

    static_assert(sizeof(FreeSlab) == templated_slab_size);
};

static SlabAllocator<8> s_slab_allocator_8;
static SlabAllocator<16> s_slab_allocator_16;
static SlabAllocator<32> s_slab_allocator_32;
static SlabAllocator<52> s_slab_allocator_52;

static_assert(sizeof(Region) <= s_slab_allocator_52.slab_size());

template<typename Callback>
void for_each_allocator(Callback callback)
{
    callback(s_slab_allocator_8);
    callback(s_slab_allocator_16);
    callback(s_slab_allocator_32);
    callback(s_slab_allocator_52);
}

void slab_alloc_init()
{
    for_each_allocator([&](auto& allocator) {
        allocator.init(128 * KB);
    });
}

void* slab_alloc(size_t slab_size)
{
    if (slab_size <= 8)
        return s_slab_allocator_8.alloc();
    if (slab_size <= 16)
        return s_slab_allocator_16.alloc();
    if (slab_size <= 32)
        return s_slab_allocator_32.alloc();
    if (slab_size <= 52)
        return s_slab_allocator_52.alloc();
    ASSERT_NOT_REACHED();
}

void slab_dealloc(void* ptr, size_t slab_size)
{
    if (slab_size <= 8)
        return s_slab_allocator_8.dealloc(ptr);
    if (slab_size <= 16)
        return s_slab_allocator_16.dealloc(ptr);
    if (slab_size <= 32)
        return s_slab_allocator_32.dealloc(ptr);
    if (slab_size <= 52)
        return s_slab_allocator_52.dealloc(ptr);
    ASSERT_NOT_REACHED();
}

void slab_alloc_stats(Function<void(size_t slab_size, size_t allocated, size_t free)> callback)
{
    for_each_allocator([&](auto& allocator) {
        callback(allocator.slab_size(), allocator.num_allocated(), allocator.num_free());
    });
}
