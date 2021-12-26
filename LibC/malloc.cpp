#include <AK/Bitmap.h>
#include <AK/InlineLinkedList.h>
#include <AK/Vector.h>
#include <sys/mman.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <serenity.h>

// FIXME: Thread safety.

//#define MALLOC_DEBUG
#define RECYCLE_BIG_ALLOCATIONS

#define MALLOC_SCRUB_BYTE 0x85
#define FREE_SCRUB_BYTE 0x82
#define MAGIC_PAGE_HEADER 0x42657274
#define MAGIC_BIGALLOC_HEADER 0x42697267
#define PAGE_ROUND_UP(x) ((((size_t)(x)) + PAGE_SIZE-1) & (~(PAGE_SIZE-1)))

static const size_t number_of_chunked_blocks_to_keep_around_per_size_class = 32;
static const size_t number_of_big_blocks_to_keep_around_per_size_class = 8;

static bool s_log_malloc = false;
static bool s_scrub_malloc = true;
static bool s_scrub_free = true;
static unsigned short size_classes[] = { 8, 16, 32, 64, 128, 252, 508, 1016, 2036, 0 };
static constexpr size_t num_size_classes = sizeof(size_classes) / sizeof(unsigned short);

struct CommonHeader {
    size_t m_magic;
    size_t m_size;
};

struct BigAllocationBlock : public CommonHeader {
    BigAllocationBlock(size_t size)
    {
        m_magic = MAGIC_BIGALLOC_HEADER;
        m_size = size;
    }
    unsigned char* m_slot[0];
};

struct FreelistEntry {
    FreelistEntry* next;
};

struct ChunkedBlock : public CommonHeader, public InlineLinkedListNode<ChunkedBlock> {
    ChunkedBlock(size_t bytes_per_chunk)
    {
        m_magic = MAGIC_PAGE_HEADER;
        m_size = bytes_per_chunk;
        m_free_chunks = chunk_capacity();
        m_freelist = (FreelistEntry*)chunk(0);
        for (size_t i = 0; i < chunk_capacity(); ++i) {
            auto* entry = (FreelistEntry*)chunk(i);
            if (i != chunk_capacity() - 1)
                entry->next = (FreelistEntry*)chunk(i + 1);
            else
                entry->next = nullptr;
        }

    }

    ChunkedBlock* m_prev { nullptr };
    ChunkedBlock* m_next { nullptr };
    FreelistEntry* m_freelist { nullptr };
    unsigned short m_free_chunks { 0 };
    unsigned char m_slot[0];

    void* chunk(int index)
    {
        return &m_slot[index * m_size];
    }
    bool is_full() const { return m_free_chunks == 0; }
    size_t bytes_per_chunk() const { return m_size; }
    size_t free_chunks() const { return m_free_chunks; }
    size_t used_chunks() const { return chunk_capacity() - m_free_chunks; }
    size_t chunk_capacity() const { return (PAGE_SIZE - sizeof(ChunkedBlock)) / m_size; }
};

struct Allocator {
    size_t size { 0 };
    size_t block_count { 0 };
    InlineLinkedList<ChunkedBlock> usable_blocks;
    InlineLinkedList<ChunkedBlock> full_blocks;
};

struct BigAllocator {
    Vector<BigAllocationBlock*, number_of_big_blocks_to_keep_around_per_size_class> blocks;
};

static Allocator g_allocators[num_size_classes];
static BigAllocator g_big_allocators[1];

static Allocator* allocator_for_size(size_t size, size_t& good_size)
{
    for (int i = 0; size_classes[i]; ++i) {
        if (size <= size_classes[i]) {
            good_size = size_classes[i];
            return &g_allocators[i];
        }
    }
    good_size = PAGE_ROUND_UP(size);
    return nullptr;
}

static BigAllocator* big_allocator_for_size(size_t size)
{
    if (size == 4096)
        return &g_big_allocators[0];
    return nullptr;
}

extern "C" {

size_t malloc_good_size(size_t size)
{
    for (int i = 0; size_classes[i]; ++i) {
        if (size < size_classes[i])
            return size_classes[i];
    }
    return PAGE_ROUND_UP(size);
}

static void* os_alloc(size_t size, const char* name)
{
    return mmap_with_name(nullptr, size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, name);
}

static void os_free(void* ptr, size_t size)
{
    int rc = munmap(ptr, size);
    assert(rc == 0);
}

void* malloc(size_t size)
{
    if (s_log_malloc)
        dbgprintf("LibC: malloc(%u)\n", size);

    if (!size)
        return nullptr;

    size_t good_size;
    auto* allocator = allocator_for_size(size, good_size);

    if (!allocator) {
        size_t real_size = PAGE_ROUND_UP(sizeof(BigAllocationBlock) + size);
#ifdef RECYCLE_BIG_ALLOCATIONS
        if (auto* allocator = big_allocator_for_size(real_size)) {
            if (!allocator->blocks.is_empty()) {
                auto* block = allocator->blocks.take_last();
                return &block->m_slot[0];
            }
        }
#endif
        char buffer[64];
        snprintf(buffer, sizeof(buffer), "malloc: BigAllocationBlock(%u)", real_size);
        auto* block = (BigAllocationBlock*)os_alloc(real_size, buffer);
        new (block) BigAllocationBlock(real_size);
        return &block->m_slot[0];
    }

    ChunkedBlock* block = nullptr;

    for (block = allocator->usable_blocks.head(); block; block = block->next()) {
        if (block->free_chunks())
            break;
    }

    if (!block) {
        char buffer[64];
        snprintf(buffer, sizeof(buffer), "malloc: ChunkedBlock(%u)", good_size);
        block = (ChunkedBlock*)os_alloc(PAGE_SIZE, buffer);
        new (block) ChunkedBlock(good_size);
        allocator->usable_blocks.append(block);
        ++allocator->block_count;
    }

    --block->m_free_chunks;
    void* ptr = block->m_freelist;
    block->m_freelist = block->m_freelist->next;
    if (block->is_full()) {
#ifdef MALLOC_DEBUG
        dbgprintf("Block %p is now full in size class %u\n", block, good_size);
#endif
        allocator->usable_blocks.remove(block);
        allocator->full_blocks.append(block);
    }
#ifdef MALLOC_DEBUG
    dbgprintf("LibC: allocated %p (chunk %d in block %p, size %u)\n", ptr, index, block, block->bytes_per_chunk());
#endif
    if (s_scrub_malloc)
        memset(ptr, MALLOC_SCRUB_BYTE, block->m_size);
    return ptr;
}

void free(void* ptr)
{
    if (!ptr)
        return;

    void* page_base = (void*)((uintptr_t)ptr & (uintptr_t)~0xfff);
    size_t magic = *(size_t*)page_base;

    if (magic == MAGIC_BIGALLOC_HEADER) {
        auto* block = (BigAllocationBlock*)page_base;
#ifdef RECYCLE_BIG_ALLOCATIONS
        if (auto* allocator = big_allocator_for_size(block->m_size)) {
            if (allocator->blocks.size() < number_of_big_blocks_to_keep_around_per_size_class) {
                allocator->blocks.append(block);
                return;
            }
        }
#endif
        os_free(block, block->m_size);
        return;
    }

    assert(magic == MAGIC_PAGE_HEADER);
    auto* block = (ChunkedBlock*)page_base;

#ifdef MALLOC_DEBUG
    dbgprintf("LibC: freeing %p in allocator %p (size=%u, used=%u)\n", ptr, page, page->bytes_per_chunk(), page->used_chunks());
#endif

    if (s_scrub_free)
        memset(ptr, FREE_SCRUB_BYTE, block->bytes_per_chunk());

    auto* entry = (FreelistEntry*)ptr;
    entry->next = block->m_freelist;
    block->m_freelist = entry;

    if (block->is_full()) {
        size_t good_size;
        auto* allocator = allocator_for_size(block->m_size, good_size);
#ifdef MALLOC_DEBUG
        dbgprintf("Block %p no longer full in size class %u\n", block, good_size);
#endif
        allocator->full_blocks.remove(block);
        allocator->usable_blocks.prepend(block);
    }

    ++block->m_free_chunks;

    if (!block->used_chunks()) {
        size_t good_size;
        auto* allocator = allocator_for_size(block->m_size, good_size);
        if (allocator->block_count < number_of_chunked_blocks_to_keep_around_per_size_class) {
#ifdef MALLOC_DEBUG
            dbgprintf("Keeping block %p around for size class %u\n", block, good_size);
#endif
            if (allocator->usable_blocks.tail() != block) {
#ifdef MALLOC_DEBUG
                dbgprintf("Moving block %p to tail of list for size class %u\n", block, good_size);
#endif
                allocator->usable_blocks.remove(block);
                allocator->usable_blocks.append(block);
            }
            return;
        }
#ifdef MALLOC_DEBUG
        dbgprintf("Releasing block %p for size class %u\n", block, good_size);
#endif
        allocator->usable_blocks.remove(block);
        --allocator->block_count;
        os_free(block, PAGE_SIZE);
    }
}

void* calloc(size_t count, size_t size)
{
    size_t new_size = count * size;
    auto* ptr = malloc(new_size);
    memset(ptr, 0, new_size);
    return ptr;
}

void* realloc(void* ptr, size_t size)
{
    if (!ptr)
        return malloc(size);

    size_t old_size = 0;
    void* page_base = (void*)((uintptr_t)ptr & (uintptr_t)~0xfff);
    auto* header = (const CommonHeader*)page_base;
    old_size = header->m_size;

    if (size == old_size)
        return ptr;
    auto* new_ptr = malloc(size);
    memcpy(new_ptr, ptr, min(old_size, size));
    free(ptr);
    return new_ptr;
}

void __malloc_init()
{
    if (getenv("LIBC_NOSCRUB_MALLOC"))
        s_scrub_malloc = false;
    if (getenv("LIBC_NOSCRUB_FREE"))
        s_scrub_free = false;
    if (getenv("LIBC_LOG_MALLOC"))
        s_log_malloc = true;
}

}

