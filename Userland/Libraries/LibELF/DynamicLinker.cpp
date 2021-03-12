/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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
#include <AK/NonnullRefPtrVector.h>
#include <AK/ScopeGuard.h>
#include <LibC/mman.h>
#include <LibC/unistd.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Hashes.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <sys/types.h>
#include <syscall.h>

namespace ELF {

namespace {
HashMap<String, NonnullRefPtr<ELF::DynamicLoader>> g_loaders;
Vector<NonnullRefPtr<ELF::DynamicObject>> g_global_objects;

using EntryPointFunction = int (*)(int, char**, char**);
using LibCExitFunction = void (*)(int);

size_t g_current_tls_offset = 0;
size_t g_total_tls_size = 0;
char** g_envp = nullptr;
LibCExitFunction g_libc_exit = nullptr;

bool g_allowed_to_check_environment_variables { false };
bool g_do_breakpoint_trap_before_entry { false };
}

Optional<DynamicObject::SymbolLookupResult> DynamicLinker::lookup_global_symbol(const StringView& symbol)
{
    Optional<DynamicObject::SymbolLookupResult> weak_result;

    auto gnu_hash = compute_gnu_hash(symbol);
    auto sysv_hash = compute_sysv_hash(symbol);

    for (auto& lib : g_global_objects) {
        auto res = lib->lookup_symbol(symbol, gnu_hash, sysv_hash);
        if (!res.has_value())
            continue;
        if (res.value().bind == STB_GLOBAL)
            return res;
        if (res.value().bind == STB_WEAK && !weak_result.has_value())
            weak_result = res;
        // We don't want to allow local symbols to be pulled in to other modules
    }
    return weak_result;
}

static void map_library(const String& name, int fd)
{
    auto loader = ELF::DynamicLoader::try_create(fd, name);
    if (!loader) {
        dbgln("Failed to create ELF::DynamicLoader for fd={}, name={}", fd, name);
        VERIFY_NOT_REACHED();
    }
    loader->set_tls_offset(g_current_tls_offset);

    g_loaders.set(name, *loader);

    g_current_tls_offset += loader->tls_size();
}

static void map_library(const String& name)
{
    // TODO: Do we want to also look for libs in other paths too?
    String path = String::formatted("/usr/lib/{}", name);
    int fd = open(path.characters(), O_RDONLY);
    VERIFY(fd >= 0);
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
    dbgln_if(DYNAMIC_LOAD_DEBUG, "mapping dependencies for: {}", name);

    for (const auto& needed_name : get_dependencies(name)) {
        dbgln_if(DYNAMIC_LOAD_DEBUG, "needed library: {}", needed_name.characters());
        String library_name = get_library_name(needed_name);

        if (!g_loaders.contains(library_name)) {
            map_library(library_name);
            map_dependencies(library_name);
        }
    }
    dbgln_if(DYNAMIC_LOAD_DEBUG, "mapped dependencies for {}", name);
}

static void allocate_tls()
{
    size_t total_tls_size = 0;
    for (const auto& data : g_loaders) {
        dbgln_if(DYNAMIC_LOAD_DEBUG, "{}: TLS Size: {}", data.key, data.value->tls_size());
        total_tls_size += data.value->tls_size();
    }
    if (total_tls_size) {
        [[maybe_unused]] void* tls_address = ::allocate_tls(total_tls_size);
        dbgln_if(DYNAMIC_LOAD_DEBUG, "from userspace, tls_address: {:p}", tls_address);
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
    auto res = libc.lookup_symbol("environ"sv);
    VERIFY(res.has_value());
    *((char***)res.value().address.as_ptr()) = g_envp;

    res = libc.lookup_symbol("__environ_is_malloced"sv);
    VERIFY(res.has_value());
    *((bool*)res.value().address.as_ptr()) = false;

    res = libc.lookup_symbol("exit"sv);
    VERIFY(res.has_value());
    g_libc_exit = (LibCExitFunction)res.value().address.as_ptr();

    res = libc.lookup_symbol("__libc_init"sv);
    VERIFY(res.has_value());
    typedef void libc_init_func();
    ((libc_init_func*)res.value().address.as_ptr())();
}

template<typename Callback>
static void for_each_dependency_of(const String& name, HashTable<String>& seen_names, Callback callback)
{
    if (seen_names.contains(name))
        return;
    seen_names.set(name);

    for (const auto& needed_name : get_dependencies(name))
        for_each_dependency_of(get_library_name(needed_name), seen_names, callback);

    callback(*g_loaders.get(name).value());
}

static NonnullRefPtrVector<DynamicLoader> collect_loaders_for_executable(const String& name)
{
    HashTable<String> seen_names;
    NonnullRefPtrVector<DynamicLoader> loaders;
    for_each_dependency_of(name, seen_names, [&](auto& loader) {
        loaders.append(loader);
    });
    return loaders;
}

static NonnullRefPtr<DynamicLoader> load_main_executable(const String& name)
{
    // NOTE: We always map the main executable first, since it may require
    //       placement at a specific address.
    auto& main_executable_loader = *g_loaders.get(name).value();
    auto main_executable_object = main_executable_loader.map();
    g_global_objects.append(*main_executable_object);

    auto loaders = collect_loaders_for_executable(name);

    for (auto& loader : loaders) {
        auto dynamic_object = loader.map();
        if (dynamic_object)
            g_global_objects.append(*dynamic_object);
    }

    for (auto& loader : loaders) {
        bool success = loader.link(RTLD_GLOBAL | RTLD_LAZY, g_total_tls_size);
        VERIFY(success);
    }

    for (auto& loader : loaders) {
        auto object = loader.load_stage_3(RTLD_GLOBAL | RTLD_LAZY, g_total_tls_size);
        VERIFY(object);

        if (loader.filename() == "libsystem.so") {
            if (syscall(SC_msyscall, object->base_address().as_ptr())) {
                VERIFY_NOT_REACHED();
            }
        }

        if (loader.filename() == "libc.so") {
            initialize_libc(*object);
        }
    }

    for (auto& loader : loaders) {
        loader.load_stage_4();
    }

    return main_executable_loader;
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

    dbgln_if(DYNAMIC_LOAD_DEBUG, "loaded all dependencies");
    for ([[maybe_unused]] auto& lib : g_loaders) {
        dbgln_if(DYNAMIC_LOAD_DEBUG, "{} - tls size: {}, tls offset: {}", lib.key, lib.value->tls_size(), lib.value->tls_offset());
    }

    allocate_tls();

    auto entry_point_function = [&main_program_name] {
        auto main_executable_loader = load_main_executable(main_program_name);
        auto entry_point = main_executable_loader->image().entry();
        if (main_executable_loader->is_dynamic())
            entry_point = entry_point.offset(main_executable_loader->text_segment_load_address().get());
        return (EntryPointFunction)(entry_point.as_ptr());
    }();

    g_loaders.clear();

    int rc = syscall(SC_msyscall, nullptr);
    if (rc < 0) {
        VERIFY_NOT_REACHED();
    }

    dbgln_if(DYNAMIC_LOAD_DEBUG, "Jumping to entry point: {:p}", entry_point_function);
    if (g_do_breakpoint_trap_before_entry) {
        asm("int3");
    }
    rc = entry_point_function(argc, argv, envp);
    dbgln_if(DYNAMIC_LOAD_DEBUG, "rc: {}", rc);
    if (g_libc_exit != nullptr) {
        g_libc_exit(rc);
    } else {
        _exit(rc);
    }

    VERIFY_NOT_REACHED();
}

}
