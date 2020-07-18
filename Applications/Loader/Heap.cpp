#include "Heap.h"
#include "Syscalls.h"
#include "Utils.h"

#define HEAP_SIZE 65536
static uint8_t s_heap_data[HEAP_SIZE];
static size_t s_heap_offset = 0;

void* malloc(size_t size)
{
    if (s_heap_offset + size >= HEAP_SIZE) {
        dbgprintf("failed to malloc with size: %d\n", size);
        exit(1);
        return nullptr;
    }
    void* result = s_heap_data + s_heap_offset;
    s_heap_offset += size;
    return result;
}

void free(void*)
{
    // does nothing
}
