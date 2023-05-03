/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/Types.h>
#include <bits/dlfcn_integration.h>
#include <dlfcn.h>
#include <string.h>

// These are filled in by the dynamic loader.
DlCloseFunction __dlclose;
DlOpenFunction __dlopen;
DlSymFunction __dlsym;
DlAddrFunction __dladdr;

// FIXME: use thread_local and a String once TLS works
#ifdef NO_TLS
char* s_dlerror_text = NULL;
bool s_dlerror_retrieved = false;
#else
__thread char* s_dlerror_text = NULL;
__thread bool s_dlerror_retrieved = false;
#endif

static void store_error(DeprecatedString const& error)
{
    free(s_dlerror_text);
    s_dlerror_text = strdup(error.characters());
    s_dlerror_retrieved = false;
}

int dlclose(void* handle)
{
    auto result = __dlclose(handle);
    if (result.is_error()) {
        store_error(result.error().text);
        return -1;
    }
    return 0;
}

char* dlerror()
{
    if (s_dlerror_retrieved) {
        free(s_dlerror_text);
        s_dlerror_text = nullptr;
    }
    s_dlerror_retrieved = true;
    return const_cast<char*>(s_dlerror_text);
}

void* dlopen(char const* filename, int flags)
{
    auto result = __dlopen(filename, flags);
    if (result.is_error()) {
        store_error(result.error().text);
        return nullptr;
    }
    return result.value();
}

void* dlsym(void* handle, char const* symbol_name)
{
    auto result = __dlsym(handle, symbol_name);
    if (result.is_error()) {
        store_error(result.error().text);
        return nullptr;
    }
    return result.value();
}

int dladdr(void const* addr, Dl_info* info)
{
    auto result = __dladdr(addr, info);
    if (result.is_error()) {
        // FIXME: According to the man page glibc does _not_ make the error
        // available via dlerror(), however we do. Does this break anything?
        store_error(result.error().text);
        return 0;
    }
    return 1;
}
