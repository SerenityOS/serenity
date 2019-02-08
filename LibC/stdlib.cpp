#include <stdlib.h>
#include <sys/mman.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <alloca.h>
#include <assert.h>
#include <errno.h>
#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/Syscall.h>

extern "C" {

#define MALLOC_SCRUB_BYTE 0x85
#define FREE_SCRUB_BYTE 0x82

struct MallocHeader {
    uint16_t first_chunk_index;
    uint16_t chunk_count;
    size_t size;

    uint32_t compute_xorcheck() const
    {
        return 0x19820413 ^ ((first_chunk_index << 16) | chunk_count) ^ size;
    }
};

struct MallocFooter {
    uint32_t xorcheck;
};

#define CHUNK_SIZE  8
#define POOL_SIZE   128 * 1024

static const size_t malloc_budget = POOL_SIZE;
static byte s_malloc_map[POOL_SIZE / CHUNK_SIZE / 8];
static byte* s_malloc_pool;

static uint32_t s_malloc_sum_alloc = 0;
static uint32_t s_malloc_sum_free = POOL_SIZE;

void* malloc(size_t size)
{
    if (size == 0)
        return nullptr;

    // We need space for the MallocHeader structure at the head of the block.
    size_t real_size = size + sizeof(MallocHeader) + sizeof(MallocFooter);

    if (s_malloc_sum_free < real_size) {
        fprintf(stderr, "malloc(): Out of memory\ns_malloc_sum_free=%u, real_size=%x\n", s_malloc_sum_free, real_size);
        assert(false);
    }

    size_t chunks_needed = real_size / CHUNK_SIZE;
    if (real_size % CHUNK_SIZE)
        chunks_needed++;

    size_t chunks_here = 0;
    size_t first_chunk = 0;

    for (unsigned i = 0; i < (POOL_SIZE / CHUNK_SIZE / 8); ++i) {
        if (s_malloc_map[i] == 0xff) {
            // Skip over completely full bucket.
            chunks_here = 0;
            continue;
        }

        // FIXME: This scan can be optimized further with TZCNT.
        for (unsigned j = 0; j < 8; ++j) {
            if ((s_malloc_map[i] & (1<<j))) {
                // This is in use, so restart chunks_here counter.
                chunks_here = 0;
                continue;
            }
            if (chunks_here == 0) {
                // Mark where potential allocation starts.
                first_chunk = i * 8 + j;
            }

            ++chunks_here;

            if (chunks_here == chunks_needed) {
                auto* header = (MallocHeader*)(s_malloc_pool + (first_chunk * CHUNK_SIZE));
                byte* ptr = ((byte*)header) + sizeof(MallocHeader);
                header->chunk_count = chunks_needed;
                header->first_chunk_index = first_chunk;
                header->size = size;

                auto* footer = (MallocFooter*)((byte*)header + (header->chunk_count * CHUNK_SIZE) - sizeof(MallocFooter));
                footer->xorcheck = header->compute_xorcheck();

                for (size_t k = first_chunk; k < (first_chunk + chunks_needed); ++k)
                    s_malloc_map[k / 8] |= 1 << (k % 8);

                s_malloc_sum_alloc += header->chunk_count * CHUNK_SIZE;
                s_malloc_sum_free  -= header->chunk_count * CHUNK_SIZE;

                memset(ptr, MALLOC_SCRUB_BYTE, (header->chunk_count * CHUNK_SIZE) - (sizeof(MallocHeader) + sizeof(MallocFooter)));
                return ptr;
            }
        }
    }

    fprintf(stderr, "malloc(): Out of memory (no consecutive chunks found for size %u)\n", size);
    volatile char* crashme = (char*)0xc007d00d;
    *crashme = 0;
    return nullptr;
}

static void validate_mallocation(void* ptr, const char* func)
{
    auto* header = (MallocHeader*)((((byte*)ptr) - sizeof(MallocHeader)));
    if (header->size == 0) {
        fprintf(stderr, "%s called on bad pointer %p, size=0\n", func, ptr);
        assert(false);
    }
    auto* footer = (MallocFooter*)((byte*)header + (header->chunk_count * CHUNK_SIZE) - sizeof(MallocFooter));
    uint32_t expected_xorcheck = header->compute_xorcheck();
    if (footer->xorcheck != expected_xorcheck) {
        fprintf(stderr, "%s called on bad pointer %p, xorcheck=%w (expected %w)\n", func, ptr, footer->xorcheck, expected_xorcheck);
        assert(false);
    }
}

void free(void* ptr)
{
    if (!ptr)
        return;

    validate_mallocation(ptr, "free()");
    auto* header = (MallocHeader*)((((byte*)ptr) - sizeof(MallocHeader)));
    for (unsigned i = header->first_chunk_index; i < (header->first_chunk_index + header->chunk_count); ++i)
        s_malloc_map[i / 8] &= ~(1 << (i % 8));

    s_malloc_sum_alloc -= header->chunk_count * CHUNK_SIZE;
    s_malloc_sum_free += header->chunk_count * CHUNK_SIZE;

    memset(header, FREE_SCRUB_BYTE, header->chunk_count * CHUNK_SIZE);
}

void __malloc_init()
{
    s_malloc_pool = (byte*)mmap(nullptr, malloc_budget, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    int rc = set_mmap_name(s_malloc_pool, malloc_budget, "malloc pool");
    if (rc < 0)
        perror("set_mmap_name failed");
}

void* calloc(size_t nmemb, size_t)
{
    (void) nmemb;
    ASSERT_NOT_REACHED();
    return nullptr;
}

void* realloc(void *ptr, size_t size)
{
    validate_mallocation(ptr, "realloc()");
    auto* header = (MallocHeader*)((((byte*)ptr) - sizeof(MallocHeader)));
    size_t old_size = header->size;
    auto* new_ptr = malloc(size);
    memcpy(new_ptr, ptr, old_size);
    return new_ptr;
}

void exit(int status)
{
    _exit(status);
    assert(false);
}

void abort()
{
    // FIXME: Implement proper abort().
    exit(253);
}

char* getenv(const char* name)
{
    for (size_t i = 0; environ[i]; ++i) {
        const char* decl = environ[i];
        char* eq = strchr(decl, '=');
        if (!eq)
            continue;
        size_t varLength = eq - decl;
        char* var = (char*)alloca(varLength + 1);
        memcpy(var, decl, varLength);
        var[varLength] = '\0';
        if (!strcmp(var, name)) {
            char* value = eq + 1;
            return value;
        }
    }
    return nullptr;
}

int atoi(const char* str)
{
    size_t len = strlen(str);
    int value = 0;
    bool isNegative = false;
    for (size_t i = 0; i < len; ++i) {
        if (i == 0 && str[0] == '-') {
            isNegative = true;
            continue;
        }
        if (str[i] < '0' || str[i] > '9')
            return value;
        value = value * 10;
        value += str[i] - '0';
    }
    return isNegative ? -value : value;
}

long atol(const char* str)
{
    static_assert(sizeof(int) == sizeof(long));
    return atoi(str);
}

static char ptsname_buf[32];
char* ptsname(int fd)
{
    if (ptsname_r(fd, ptsname_buf, sizeof(ptsname_buf)) < 0)
        return nullptr;
    return ptsname_buf;
}

int ptsname_r(int fd, char* buffer, size_t size)
{
    int rc = syscall(SC_ptsname_r, fd, buffer, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

static unsigned long s_next_rand = 1;

int rand()
{
    s_next_rand = s_next_rand * 1103515245 + 12345;
    return((unsigned)(s_next_rand/((RAND_MAX + 1) * 2)) % (RAND_MAX + 1));
}

void srand(unsigned seed)
{
    s_next_rand = seed;
}

int abs(int i)
{
    return i < 0 ? -i : i;
}

long int random()
{
    return rand();
}

void srandom(unsigned seed)
{
    srand(seed);
}

int system(const char* command)
{
    return execl("/bin/sh", "sh", "-c", command, nullptr);
}

char* mktemp(char*)
{
    ASSERT_NOT_REACHED();
}

void* bsearch(const void* key, const void* base, size_t nmemb, size_t size, int (*compar)(const void *, const void *))
{
    dbgprintf("FIXME(LibC): bsearch(%p, %p, %u, %u, %p)\n", key, base, nmemb, size, compar);
    ASSERT_NOT_REACHED();
}

div_t div(int numerator, int denominator)
{
    div_t result;
    result.quot = numerator / denominator;
    result.rem = numerator % denominator;
    return result;
}

ldiv_t ldiv(long numerator, long denominator)
{
    ldiv_t result;
    result.quot = numerator / denominator;
    result.rem = numerator % denominator;
    return result;
}

}
