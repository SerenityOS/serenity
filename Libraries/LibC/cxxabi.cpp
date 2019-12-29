#include <AK/Types.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>

//#define GLOBAL_DTORS_DEBUG

extern "C" {

typedef void (*AtExitFunction)(void*);

struct __exit_entry {
    AtExitFunction method;
    void* parameter;
    void* dso_handle;
    bool has_been_called;
};

static __exit_entry __exit_entries[1024] {};
static int __exit_entry_count = 0;

int __cxa_atexit(AtExitFunction exit_function, void* parameter, void* dso_handle)
{
    if (__exit_entry_count >= 1024)
        return -1;

    __exit_entries[__exit_entry_count++] = { exit_function, parameter, dso_handle, false };

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

#ifdef GLOBAL_DTORS_DEBUG
    dbgprintf("__cxa_finalize: %d entries in the finalizer list\n", entry_index);
#endif

    while (--entry_index >= 0) {
        auto& exit_entry = __exit_entries[entry_index];
        bool needs_calling = !exit_entry.has_been_called && (!dso_handle || dso_handle == exit_entry.dso_handle);
        if (needs_calling) {
#ifdef GLOBAL_DTORS_DEBUG
            dbgprintf("__cxa_finalize: calling entry[%d] %p(%p) dso: %p\n", entry_index, exit_entry.method, exit_entry.parameter, exit_entry.dso_handle);
#endif
            exit_entry.method(exit_entry.parameter);
            exit_entry.has_been_called = true;
        }
    }
}

} // extern "C"
