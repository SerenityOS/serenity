/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/LogStream.h>
#include <AK/ScopeGuard.h>
#include <LibC/mman.h>
#include <LibC/stdio.h>
#include <LibC/sys/internals.h>
#include <LibC/unistd.h>
#include <LibCore/File.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Image.h>
#include <LibELF/exec_elf.h>
#include <dlfcn.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>

namespace ELF {

namespace {
HashMap<String, NonnullRefPtr<ELF::DynamicLoader>> g_loaders;
HashMap<String, NonnullRefPtr<ELF::DynamicObject>> g_loaded_objects;
Vector<NonnullRefPtr<ELF::DynamicObject>> g_global_objects;

using MainFunction = int (*)(int, char**, char**);
using LibCExitFunction = void (*)(int);

size_t g_current_tls_offset = 0;
size_t g_total_tls_size = 0;
char** g_envp = nullptr;
LibCExitFunction g_libc_exit = nullptr;

bool g_allowed_to_check_environment_variables { false };
bool g_do_breakpoint_trap_before_entry { false };
}

DynamicObject::SymbolLookupResult DynamicLinker::lookup_global_symbol(const char* symbol_name)
{
    DynamicObject::SymbolLookupResult weak_result = {};
    for (auto& lib : g_global_objects) {
        auto res = lib->lookup_symbol(symbol_name);
        if (res.found) {
            if (res.bind == STB_GLOBAL) {
                return res;
            } else if (res.bind == STB_WEAK && !weak_result.found) {
                weak_result = res;
            }
            // We don't want to allow local symbols to be pulled in to other modules
        }
    }
    return weak_result;
}

static void map_library(const String& name, int fd)
{
    struct stat lib_stat;
    int rc = fstat(fd, &lib_stat);
    ASSERT(!rc);

    auto loader = ELF::DynamicLoader::construct(name.characters(), fd, lib_stat.st_size);
    loader->set_tls_offset(g_current_tls_offset);

    g_loaders.set(name, loader);

    g_current_tls_offset += loader->tls_size();
}

static void map_library(const String& name)
{
    // TODO: Do we want to also look for libs in other paths too?
    String path = String::format("/usr/lib/%s", name.characters());
    int fd = open(path.characters(), O_RDONLY);
    ASSERT(fd >= 0);
    map_library(name, fd);
}

static String get_library_name(const StringView& path)
{
    return LexicalPath(path).basename();
}

static Vector<String> get_dependencies(const String& name)
{
    auto lib = g_loaders.get(name).value();
    Vector<String> dependencies;

    lib->for_each_needed_library([&dependencies, &name](auto needed_name) {
        if (name == needed_name)
            return IterationDecision::Continue;
        dependencies.append(needed_name);
        return IterationDecision::Continue;
    });
    return dependencies;
}

static void map_dependencies(const String& name)
{
    dbgln<DYNAMIC_LOAD_DEBUG>("mapping dependencies for: {}", name);

    for (const auto& needed_name : get_dependencies(name)) {
        dbgln<DYNAMIC_LOAD_DEBUG>("needed library: {}", needed_name.characters());
        String library_name = get_library_name(needed_name);

        if (!g_loaders.contains(library_name)) {
            map_library(library_name);
            map_dependencies(library_name);
        }
    }
    dbgln<DYNAMIC_LOAD_DEBUG>("mapped dependencies for {}", name);
}

static void allocate_tls()
{
    size_t total_tls_size = 0;
    for (const auto& data : g_loaders) {
        dbgln<DYNAMIC_LOAD_DEBUG>("{}: TLS Size: {}", data.key, data.value->tls_size());
        total_tls_size += data.value->tls_size();
    }
    if (total_tls_size) {
        [[maybe_unused]] void* tls_address = ::allocate_tls(total_tls_size);
        dbgln<DYNAMIC_LOAD_DEBUG>("from userspace, tls_address: {:p}", tls_address);
    }
    g_total_tls_size = total_tls_size;
}

static void initialize_libc(DynamicObject& libc)
{
    // Traditionally, `_start` of the main program initializes libc.
    // However, since some libs use malloc() and getenv() in global constructors,
    // we have to initialize libc just after it is loaded.
    // Also, we can't just mark `__libc_init` with "__attribute__((constructor))"
    // because it uses getenv() internally, so `environ` has to be initialized before we call `__libc_init`.
    auto res = libc.lookup_symbol("environ");
    ASSERT(res.found);
    *((char***)res.address) = g_envp;

    res = libc.lookup_symbol("__environ_is_malloced");
    ASSERT(res.found);
    *((bool*)res.address) = false;

    res = libc.lookup_symbol("exit");
    ASSERT(res.found);
    g_libc_exit = (LibCExitFunction)res.address;

    res = libc.lookup_symbol("__libc_init");
    ASSERT(res.found);
    typedef void libc_init_func();
    ((libc_init_func*)res.address)();
}

static void load_elf(const String& name)
{
    dbgln<DYNAMIC_LOAD_DEBUG>("load_elf: {}", name);
    auto loader = g_loaders.get(name).value();
    for (const auto& needed_name : get_dependencies(name)) {
        dbgln<DYNAMIC_LOAD_DEBUG>("needed library: {}", needed_name);
        String library_name = get_library_name(needed_name);
        if (!g_loaded_objects.contains(library_name)) {
            load_elf(library_name);
        }
    }

    auto dynamic_object = loader->load_from_image(RTLD_GLOBAL | RTLD_LAZY, g_total_tls_size);
    ASSERT(dynamic_object);

    g_loaded_objects.set(name, *dynamic_object);
    g_global_objects.append(*dynamic_object);

    dbgln<DYNAMIC_LOAD_DEBUG>("load_elf: done {}", name);
}

static NonnullRefPtr<DynamicLoader> commit_elf(const String& name)
{
    auto loader = g_loaders.get(name).value();
    for (const auto& needed_name : get_dependencies(name)) {
        String library_name = get_library_name(needed_name);
        if (g_loaders.contains(library_name)) {
            commit_elf(library_name);
        }
    }

    auto object = loader->load_stage_3(RTLD_GLOBAL | RTLD_LAZY, g_total_tls_size);
    ASSERT(object);
    if (name == "libc.so") {
        initialize_libc(*object);
    }
    g_loaders.remove(name);
    return loader;
}

static void read_environment_variables()
{
    for (char** env = g_envp; *env; ++env) {
        if (StringView { *env } == "_LOADER_BREAKPOINT=1") {
            g_do_breakpoint_trap_before_entry = true;
        }
    }
}

void ELF::DynamicLinker::linker_main(String&& main_program_name, int main_program_fd, bool is_secure, int argc, char** argv, char** envp)
{
    g_envp = envp;

    g_allowed_to_check_environment_variables = !is_secure;
    if (g_allowed_to_check_environment_variables)
        read_environment_variables();

    map_library(main_program_name, main_program_fd);
    map_dependencies(main_program_name);

    dbgln<DYNAMIC_LOAD_DEBUG>("loaded all dependencies");
    for ([[maybe_unused]] auto& lib : g_loaders) {
        dbgln<DYNAMIC_LOAD_DEBUG>("{} - tls size: {}, tls offset: {}", lib.key, lib.value->tls_size(), lib.value->tls_offset());
    }

    allocate_tls();

    load_elf(main_program_name);
    auto main_program_lib = commit_elf(main_program_name);

    FlatPtr entry_point = reinterpret_cast<FlatPtr>(main_program_lib->image().entry().as_ptr());
    if (main_program_lib->is_dynamic())
        entry_point += reinterpret_cast<FlatPtr>(main_program_lib->text_segment_load_address().as_ptr());

    dbgln<DYNAMIC_LOAD_DEBUG>("entry point: {:p}", (void*)entry_point);
    g_loaders.clear();

    MainFunction main_function = (MainFunction)(entry_point);
    dbgln<DYNAMIC_LOAD_DEBUG>("jumping to main program entry point: {:p}", main_function);
    if (g_do_breakpoint_trap_before_entry) {
        asm("int3");
    }
    int rc = main_function(argc, argv, envp);
    dbgln<DYNAMIC_LOAD_DEBUG>("rc: {}", rc);
    if (g_libc_exit != nullptr) {
        g_libc_exit(rc);
    } else {
        _exit(rc);
    }

    ASSERT_NOT_REACHED();
}

}
