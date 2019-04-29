#include <stdlib.h>
#include <sys/mman.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <alloca.h>
#include <assert.h>
#include <errno.h>
#include <ctype.h>
#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/Syscall.h>
#include <AK/StdLibExtras.h>
#include <AK/HashMap.h>
#include <AK/AKString.h>

extern "C" {

#define MALLOC_SCRUB_BYTE 0x85
#define FREE_SCRUB_BYTE 0x82

struct MallocHeader {
    uint16_t first_chunk_index;
    uint16_t chunk_count : 15;
    bool is_mmap : 1;
    size_t size;
};

#define CHUNK_SIZE  32
#define POOL_SIZE   4 * 1048576

static const size_t malloc_budget = POOL_SIZE;
static byte s_malloc_map[POOL_SIZE / CHUNK_SIZE / 8];
static byte* s_malloc_pool;

static uint32_t s_malloc_sum_alloc = 0;
static uint32_t s_malloc_sum_free = POOL_SIZE;

static bool s_log_malloc = false;
static bool s_scrub_malloc = true;
static bool s_scrub_free = true;

void* malloc(size_t size)
{
    if (s_log_malloc)
        dbgprintf("LibC: malloc(%u)\n", size);

    if (size == 0)
        return nullptr;

    // We need space for the MallocHeader structure at the head of the block.
    size_t real_size = size + sizeof(MallocHeader);

    if (real_size >= PAGE_SIZE) {
        auto* memory = mmap(nullptr, real_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
        if (memory == MAP_FAILED) {
            fprintf(stderr, "malloc() failed to mmap() for a %u-byte allocation: %s", size, strerror(errno));
            volatile char* crashme = (char*)0xf007d00d;
            *crashme = 0;
            return nullptr;
        }
        auto* header = (MallocHeader*)(memory);
        byte* ptr = ((byte*)header) + sizeof(MallocHeader);
        header->chunk_count = 0;
        header->first_chunk_index = 0;
        header->size = real_size;
        header->is_mmap = true;
        return ptr;
    }

    if (s_malloc_sum_free < real_size) {
        fprintf(stderr, "malloc(): Out of memory\ns_malloc_sum_free=%u, real_size=%u\n", s_malloc_sum_free, real_size);
        ASSERT_NOT_REACHED();
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
                header->is_mmap = false;
                header->size = size;

                for (size_t k = first_chunk; k < (first_chunk + chunks_needed); ++k)
                    s_malloc_map[k / 8] |= 1 << (k % 8);

                s_malloc_sum_alloc += header->chunk_count * CHUNK_SIZE;
                s_malloc_sum_free  -= header->chunk_count * CHUNK_SIZE;

                if (s_scrub_malloc)
                    memset(ptr, MALLOC_SCRUB_BYTE, (header->chunk_count * CHUNK_SIZE) - sizeof(MallocHeader));
                return ptr;
            }
        }
    }

    fprintf(stderr, "malloc(): Out of memory (no consecutive chunks found for size %u)\n", size);
    volatile char* crashme = (char*)0xc007d00d;
    *crashme = 0;
    return nullptr;
}

void free(void* ptr)
{
    if (!ptr)
        return;

    auto* header = (MallocHeader*)((((byte*)ptr) - sizeof(MallocHeader)));
    if (header->is_mmap) {
        int rc = munmap(header, header->size);
        if (rc < 0)
            fprintf(stderr, "free(): munmap(%p) for allocation %p with size %u failed: %s\n", header, ptr, header->size, strerror(errno));
        return;
    }

    for (int i = header->first_chunk_index; i < (header->first_chunk_index + header->chunk_count); ++i)
        s_malloc_map[i / 8] &= ~(1 << (i % 8));

    s_malloc_sum_alloc -= header->chunk_count * CHUNK_SIZE;
    s_malloc_sum_free += header->chunk_count * CHUNK_SIZE;

    if (s_scrub_free)
        memset(header, FREE_SCRUB_BYTE, header->chunk_count * CHUNK_SIZE);
}

void __malloc_init()
{
    s_malloc_pool = (byte*)mmap(nullptr, malloc_budget, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    int rc = set_mmap_name(s_malloc_pool, malloc_budget, "malloc pool");
    if (rc < 0)
        perror("set_mmap_name failed");

    if (getenv("LIBC_NOSCRUB_MALLOC"))
        s_scrub_malloc = false;
    if (getenv("LIBC_NOSCRUB_FREE"))
        s_scrub_free = false;
    if (getenv("LIBC_LOG_MALLOC"))
        s_log_malloc = true;
}

void* calloc(size_t count, size_t size)
{
    size_t new_size = count * size;
    auto* ptr = malloc(new_size);
    memset(ptr, 0, new_size);
    return ptr;
}

void* realloc(void *ptr, size_t size)
{
    if (!ptr)
        return malloc(size);
    auto* header = (MallocHeader*)((((byte*)ptr) - sizeof(MallocHeader)));
    size_t old_size = header->size;
    if (size == old_size)
        return ptr;
    auto* new_ptr = malloc(size);
    memcpy(new_ptr, ptr, min(old_size, size));
    free(ptr);
    return new_ptr;
}

typedef void(*__atexit_handler)();
static int __atexit_handler_count = 0;
static __atexit_handler __atexit_handlers[32];

void exit(int status)
{
    for (int i = 0; i < __atexit_handler_count; ++i)
        __atexit_handlers[i]();
    extern void _fini();
    _fini();
    _exit(status);
    ASSERT_NOT_REACHED();
}

int atexit(void (*handler)())
{
    ASSERT(__atexit_handler_count < 32);
    __atexit_handlers[__atexit_handler_count++] = handler;
    return 0;
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

int putenv(char* new_var)
{
    HashMap<String, String> environment;

    auto handle_environment_entry = [&environment] (const char* decl) {
        char* eq = strchr(decl, '=');
        if (!eq)
            return;
        size_t var_length = eq - decl;
        char* var = (char*)alloca(var_length + 1);
        memcpy(var, decl, var_length);
        var[var_length] = '\0';
        const char* value = eq + 1;
        environment.set(var, value);
    };
    for (size_t i = 0; environ[i]; ++i)
        handle_environment_entry(environ[i]);
    handle_environment_entry(new_var);

    //extern bool __environ_is_malloced;
    //if (__environ_is_malloced)
    //    free(environ);
    //__environ_is_malloced = true;

    int environment_size = sizeof(char*); // For the null sentinel.
    for (auto& it : environment)
        environment_size += (int)sizeof(char*) + it.key.length() + 1 + it.value.length() + 1;

    char* buffer = (char*)malloc(environment_size);
    environ = (char**)buffer;
    char* bufptr = buffer + sizeof(char*) * (environment.size() + 1);

    int i = 0;
    for (auto& it : environment) {
        environ[i] = bufptr;
        memcpy(bufptr, it.key.characters(), it.key.length());
        bufptr += it.key.length();
        *(bufptr++) = '=';
        memcpy(bufptr, it.value.characters(), it.value.length());
        bufptr += it.value.length();
        *(bufptr++) = '\0';
        ++i;
    }
    environ[environment.size()] = nullptr;
    return 0;
}

double strtod(const char* str, char** endptr)
{
    (void)str;
    (void)endptr;
    dbgprintf("LibC: strtod: '%s'\n", str);
    ASSERT_NOT_REACHED();
}

float strtof(const char* str, char** endptr)
{
    (void)str;
    (void)endptr;
    dbgprintf("LibC: strtof: '%s'\n", str);
    ASSERT_NOT_REACHED();
}

double atof(const char* str)
{
    dbgprintf("LibC: atof: '%s'\n", str);
    ASSERT_NOT_REACHED();
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

long long atoll(const char* str)
{
    dbgprintf("FIXME(Libc): atoll('%s') passing through to atol()\n", str);
    return atol(str);
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

char* mktemp(char* pattern)
{
    int length = strlen(pattern);

    // FIXME: Check for an invalid template pattern and return EINVAL.
    if (length < 6) {
        pattern[0] = '\0';
        errno = EINVAL;
        return pattern;
    }

    int start = length - 6;

    static constexpr char random_characters[] = "abcdefghijklmnopqrstuvwxyz0123456789";

    for (int attempt = 0; attempt < 100; ++attempt) {
        for (int i = 0; i < 6; ++i)
            pattern[start + i] = random_characters[(rand() % sizeof(random_characters))];
        struct stat st;
        int rc = lstat(pattern, &st);
        if (rc < 0 && errno == ENOENT)
            return pattern;
    }
    pattern[0] = '\0';
    errno = EEXIST;
    return pattern;
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

size_t mbstowcs(wchar_t*, const char*, size_t)
{
    ASSERT_NOT_REACHED();
}

long strtol(const char* str, char** endptr, int base)
{
    const char* s = str;
    unsigned long acc;
    int c;
    unsigned long cutoff;
    int neg = 0;
    int any;
    int cutlim;

    do {
        c = *s++;
    } while (isspace(c));
    if (c == '-') {
        neg = 1;
        c = *s++;
    } else if (c == '+')
        c = *s++;
    if ((base == 0 || base == 16) &&
        c == '0' && (*s == 'x' || *s == 'X')) {
        c = s[1];
        s += 2;
        base = 16;
    }
    if (base == 0)
        base = c == '0' ? 8 : 10;

    cutoff = neg ? -(unsigned long)LONG_MIN : LONG_MAX;
    cutlim = cutoff % (unsigned long)base;
    cutoff /= (unsigned long)base;
    for (acc = 0, any = 0;; c = *s++) {
        if (isdigit(c))
            c -= '0';
        else if (isalpha(c))
            c -= isupper(c) ? 'A' - 10 : 'a' - 10;
        else
            break;
        if (c >= base)
            break;
        if (any < 0 || acc > cutoff || (acc == cutoff && c > cutlim))
            any = -1;
        else {
            any = 1;
            acc *= base;
            acc += c;
        }
    }
    if (any < 0) {
        acc = neg ? LONG_MIN : LONG_MAX;
        errno = ERANGE;
    } else if (neg)
        acc = -acc;
    if (endptr)
        *endptr = const_cast<char*>((any ? s - 1 : str));
    return acc;
}

unsigned long strtoul(const char* str, char** endptr, int base)
{
    auto value = strtol(str, endptr, base);
    ASSERT(value >= 0);
    return value;
}

}
