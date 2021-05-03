/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/HashMap.h>
#include <AK/LexicalPath.h>
#include <AK/RefPtr.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <LibELF/DynamicLoader.h>
#include <assert.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <unistd.h>

// NOTE: The string here should never include a trailing newline (according to POSIX)
String g_dlerror_msg;

HashMap<String, RefPtr<ELF::DynamicLoader>> g_elf_objects;

extern "C" {

int dlclose(void*)
{
    g_dlerror_msg = "dlclose not implemented!";
    return -1;
}

char* dlerror()
{
    return const_cast<char*>(g_dlerror_msg.characters());
}

void* dlopen(const char* filename, int flags)
{
    // FIXME: Create a global mutex/semaphore/lock for dlopen/dlclose/dlsym and (?) dlerror
    // FIXME: refcount?

    if (!filename) {
        // FIXME: Return the handle for "the main executable"
        //     The Serenity Kernel will keep a mapping of the main elf binary resident in memory,
        //     But a future dynamic loader might have a different idea/way of letting us access this information
        TODO();
    }

    auto basename = LexicalPath(filename).basename();

    auto existing_elf_object = g_elf_objects.get(basename);
    if (existing_elf_object.has_value()) {
        return const_cast<ELF::DynamicLoader*>(existing_elf_object.value());
    }

    int fd = open(filename, O_RDONLY);
    if (fd < 0) {
        g_dlerror_msg = String::formatted("Unable to open file {}", filename);
        return nullptr;
    }

    ScopeGuard close_fd_guard([fd]() { close(fd); });

    auto loader = ELF::DynamicLoader::try_create(fd, filename);
    if (!loader || !loader->is_valid()) {
        g_dlerror_msg = String::formatted("{} is not a valid ELF dynamic shared object!", filename);
        return nullptr;
    }

    auto object = loader->map();
    if (!object || !loader->link(flags, /* total_tls_size (FIXME) */ 0)) {
        g_dlerror_msg = String::formatted("Failed to load ELF object {}", filename);
        return nullptr;
    }

    g_elf_objects.set(basename, move(loader));
    g_dlerror_msg = "Successfully loaded ELF object.";

    // we have one refcount already
    return const_cast<ELF::DynamicLoader*>(g_elf_objects.get(basename).value());
}

void* dlsym(void* handle, const char* symbol_name)
{
    // FIXME: When called with a NULL handle we're supposed to search every dso in the process... that'll get expensive
    VERIFY(handle);
    auto* dso = reinterpret_cast<ELF::DynamicLoader*>(handle);
    void* symbol = dso->symbol_for_name(symbol_name);
    if (!symbol) {
        g_dlerror_msg = "Symbol not found";
        return nullptr;
    }
    return symbol;
}

} // extern "C"
