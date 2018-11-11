#include <stdlib.h>
#include <sys/mman.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <alloca.h>
#include <assert.h>
#include <Kernel/Syscall.h>
#include <AK/Assertions.h>

extern "C" {

// FIXME: This is a temporary malloc() implementation. It never frees anything,
//        and you can't allocate more than 128 kB total.
static const size_t mallocBudget = 131072;

static byte* nextptr = nullptr;
static byte* endptr = nullptr;

void __malloc_init()
{
    nextptr = (byte*)mmap(nullptr, mallocBudget, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    endptr = nextptr + mallocBudget;
    int rc = set_mmap_name(nextptr, mallocBudget, "malloc");
    if (rc < 0)
        perror("set_mmap_name failed");
}

void* malloc(size_t size)
{
    if ((nextptr + size) > endptr) {
        volatile char* crashme = (char*)0xc007d00d;
        *crashme = 0;
    }
    byte* ret = nextptr;
    nextptr += size;
    nextptr += 16;
    nextptr = (byte*)((dword)nextptr & 0xfffffff0);
    return ret;
}

void free(void* ptr)
{
    if (!ptr)
        return;
#if 0
    munmap(ptr, 4096);
#endif
}

void* calloc(size_t nmemb, size_t)
{
    (void) nmemb;
    ASSERT_NOT_REACHED();
    return nullptr;
}

void* realloc(void *ptr, size_t size)
{
    // FIXME: This is broken as shit.
    auto* new_ptr = malloc(size);
    memcpy(new_ptr, ptr, size);
    return new_ptr;
}

void exit(int status)
{
    Syscall::invoke(Syscall::SC_exit, (dword)status);
    for (;;);
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

void __qsort(void *base, size_t nmemb, size_t size, int (*compar)(const void *, const void *))
{
    (void) base;
    (void) nmemb;
    (void) size;
    (void) compar;
    assert(false);
}

}
