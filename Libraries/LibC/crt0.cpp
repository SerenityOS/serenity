#include <AK/Types.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>

extern "C" {

int main(int, char**, char**);

__thread int errno;
char** environ;
bool __environ_is_malloced;

void __libc_init()
{
    void __malloc_init();
    __malloc_init();

    void __stdio_init();
    __stdio_init();
}

int _start(int argc, char** argv, char** env)
{
    environ = env;
    __environ_is_malloced = false;

    __libc_init();

    extern void _init();
    _init();

    extern void (*__init_array_start[])(int, char**, char**) __attribute__((visibility("hidden")));
    extern void (*__init_array_end[])(int, char**, char**) __attribute__((visibility("hidden")));

    const size_t size = __init_array_end - __init_array_start;
    for (size_t i = 0; i < size; i++)
        (*__init_array_start[i])(argc, argv, env);

    int status = main(argc, argv, environ);

    exit(status);

    return 20150614;
}

[[noreturn]] void __cxa_pure_virtual()
{
    ASSERT_NOT_REACHED();
}

typedef void (*AtExitFunction)(void *);

struct __exit_entry
{
    AtExitFunction method;
    void* parameter;
    void* dso_handle;
    bool has_been_called;
};

static __exit_entry __exit_entries[1024]{};
static int __exit_entry_count = 0;

int __cxa_atexit(AtExitFunction exit_function, void *parameter, void *dso_handle)
{
    if (__exit_entry_count >= 1024)
        return -1;

    __exit_entries[__exit_entry_count++] = { exit_function, parameter, dso_handle, false};

    return 0;
}

void __cxa_finalize(void* dso_handle)
{
    // From the itanium abi, https://itanium-cxx-abi.github.io/cxx-abi/abi.html#dso-dtor-runtime-api
    //
    // When __cxa_finalize(d) is called, it should walk the termination function list, calling each in turn
    // if d matches __dso_handle for the termination function entry. If d == NULL, it should call all of them.
    // Multiple calls to __cxa_finalize shall not result in calling termination function entries multiple times;
    // the implementation may either remove entries or mark them finished.

    int entry_index = __exit_entry_count;

    while (--entry_index >= 0)
    {
        auto& exit_entry = __exit_entries[entry_index];
        bool needs_calling = !exit_entry.has_been_called && (!dso_handle || dso_handle == exit_entry.dso_handle);
        if (needs_calling) {
            exit_entry.method(exit_entry.parameter);
            exit_entry.has_been_called = true;
        }
    }
}

extern u32 __stack_chk_guard;
u32 __stack_chk_guard = (u32)0xc0000c13;

[[noreturn]] void __stack_chk_fail()
{
    ASSERT_NOT_REACHED();
}
}
