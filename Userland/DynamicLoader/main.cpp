/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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
#include <LibELF/DynamicLoader.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Image.h>
#include <LibELF/Loader.h>
#include <LibELF/exec_elf.h>
#include <dlfcn.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>

// #define DYNAMIC_LOAD_VERBOSE

#ifdef DYNAMIC_LOAD_VERBOSE
#    define VERBOSE(fmt, ...) dbgprintf(fmt, ##__VA_ARGS__)
#else
#    define VERBOSE(fmt, ...) \
        do {                  \
        } while (0)
#endif
#define TLS_VERBOSE(fmt, ...) dbgprintf(fmt, ##__VA_ARGS__)

char* __static_environ[] = { nullptr }; // We don't get the environment without some libc workarounds..

static HashMap<String, NonnullRefPtr<ELF::DynamicLoader>> g_loaders;
static HashMap<String, NonnullRefPtr<ELF::DynamicObject>> g_loaded_objects;

static size_t g_current_tls_offset = 0;
static size_t g_total_tls_size = 0;
static char** g_envp = nullptr;

static void init_libc()
{
    environ = __static_environ;
    __environ_is_malloced = false;
    __stdio_is_initialized = false;
    __malloc_init();
}

static void perform_self_relocations(auxv_t* auxvp)
{
    // We need to relocate ourselves.
    // (these relocations seem to be generated because of our vtables)

    FlatPtr base_address = 0;
    bool found_base_address = false;
    for (; auxvp->a_type != AT_NULL; ++auxvp) {
        if (auxvp->a_type == AuxiliaryValue::BaseAddress) {
            base_address = auxvp->a_un.a_val;
            found_base_address = true;
        }
    }
    ASSERT(found_base_address);
    Elf32_Ehdr* header = (Elf32_Ehdr*)(base_address);
    Elf32_Phdr* pheader = (Elf32_Phdr*)(base_address + header->e_phoff);
    u32 dynamic_section_addr = 0;
    for (size_t i = 0; i < (size_t)header->e_phnum; ++i, ++pheader) {
        if (pheader->p_type != PT_DYNAMIC)
            continue;
        dynamic_section_addr = pheader->p_vaddr + base_address;
    }
    if (!dynamic_section_addr)
        exit(1);

    auto dynamic_object = ELF::DynamicObject::construct((VirtualAddress(base_address)), (VirtualAddress(dynamic_section_addr)));

    dynamic_object->relocation_section().for_each_relocation([base_address](auto& reloc) {
        if (reloc.type() != R_386_RELATIVE)
            return IterationDecision::Continue;
        *(u32*)reloc.address().as_ptr() += base_address;
        return IterationDecision::Continue;
    });
}

static ELF::DynamicObject::SymbolLookupResult global_symbol_lookup(const char* symbol_name)
{
    VERBOSE("global symbol lookup: %s\n", symbol_name);
    for (auto& lib : g_loaded_objects) {
        VERBOSE("looking up in object: %s\n", lib.key.characters());
        auto res = lib.value->lookup_symbol(symbol_name);
        if (!res.has_value())
            continue;
        return res.value();
    }
    // ASSERT_NOT_REACHED();
    return {};
}

static void map_library(const String& name, int fd)
{
    struct stat lib_stat;
    int rc = fstat(fd, &lib_stat);
    ASSERT(!rc);

    auto loader = ELF::DynamicLoader::construct(name.characters(), fd, lib_stat.st_size);
    loader->set_tls_offset(g_current_tls_offset);
    loader->set_global_symbol_lookup_function(global_symbol_lookup);

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

    lib->for_each_needed_library([&dependencies](auto needed_name) {
        dependencies.append(needed_name);
        return IterationDecision::Continue;
    });
    return dependencies;
}

static void map_dependencies(const String& name)
{
    VERBOSE("mapping dependencies for: %s", name.characters());

    for (const auto& needed_name : get_dependencies(name)) {
        VERBOSE("needed library: %s", needed_name.characters());
        String library_name = get_library_name(needed_name);

        if (!g_loaders.contains(library_name)) {
            map_library(library_name);
            map_dependencies(library_name);
        }
    }
}

static void allocate_tls()
{
    size_t total_tls_size = 0;
    for (const auto& data : g_loaders) {
        VERBOSE("%s: TLS Size: %u\n", data.key.characters(), data.value->tls_size());
        total_tls_size += data.value->tls_size();
    }
    if (total_tls_size) {
        void* tls_address = allocate_tls(total_tls_size);
        (void)tls_address;
        VERBOSE("from userspace, tls_address: %p", tls_address);
    }
    g_total_tls_size = total_tls_size;
}

static void initialize_libc()
{
    // Traditionally, `_start` of the main program initializes libc.
    // However, since some libs use malloc() and getenv() in global constructors,
    // we have to initialize libc just after it is loaded.
    // Also, we can't just mark `__libc_init` with "__attribute__((constructor))"
    // because it uses getenv() internally, so `environ` has to be initialized before we call `__libc_init`.
    auto res = global_symbol_lookup("environ");
    *((char***)res.address) = g_envp;
    ASSERT(res.found);
    res = global_symbol_lookup("__environ_is_malloced");
    ASSERT(res.found);
    *((bool*)res.address) = false;

    res = global_symbol_lookup("__libc_init");
    ASSERT(res.found);
    typedef void libc_init_func(void);
    ((libc_init_func*)res.address)();
}

static void load_elf(const String& name)
{
    VERBOSE("load_elf: %s", name.characters());
    auto loader = g_loaders.get(name).value();
    for (const auto& needed_name : get_dependencies(name)) {
        VERBOSE("needed library: %s", needed_name.characters());
        String library_name = get_library_name(needed_name);
        if (!g_loaded_objects.contains(library_name)) {
            load_elf(library_name);
        }
    }

    VERBOSE("loading: %s", name.characters());
    auto dynamic_object = loader->load_from_image(RTLD_GLOBAL | RTLD_LAZY, g_total_tls_size);
    ASSERT(!dynamic_object.is_null());
    g_loaded_objects.set(name, dynamic_object.release_nonnull());

    if (name == "libc.so") {
        initialize_libc();
    }
}

static void clear_temporary_objects_mappings()
{

    g_loaders.clear();
}

static FlatPtr loader_main(auxv_t* auxvp)
{
    int main_program_fd = -1;
    String main_program_name;
    for (; auxvp->a_type != AT_NULL; ++auxvp) {
        if (auxvp->a_type == AuxiliaryValue::ExecFileDescriptor) {
            main_program_fd = auxvp->a_un.a_val;
        }
        if (auxvp->a_type == AuxiliaryValue::ExecFilename) {
            main_program_name = (const char*)auxvp->a_un.a_ptr;
        }
    }
    ASSERT(main_program_fd >= 0);
    ASSERT(!main_program_name.is_null());
    dbgln("loading: {}", main_program_name);

    map_library(main_program_name, main_program_fd);
    map_dependencies(main_program_name);

    VERBOSE("loaded all dependencies");
    for (auto& lib : g_loaders) {
        (void)lib;
        VERBOSE("%s - tls size: $u, tls offset: %u", lib.key.characters(), lib.value->tls_size(), lib.value->tls_offset());
    }

    allocate_tls();

    load_elf(main_program_name);

    auto main_program_lib = g_loaders.get(main_program_name).value();
    FlatPtr entry_point = reinterpret_cast<FlatPtr>(main_program_lib->image().entry().as_ptr() + (FlatPtr)main_program_lib->text_segment_load_address().as_ptr());
    VERBOSE("entry point: %p", entry_point);

    // This will unmap the temporary memory maps we had for loading the libraries
    clear_temporary_objects_mappings();

    return entry_point;
}

extern "C" {

// The compiler expects a previous declaration
void _start(int, char**, char**);

using MainFunction = int (*)(int, char**, char**);

void _start(int argc, char** argv, char** envp)
{
    g_envp = envp;
    char** env;
    for (env = envp; *env; ++env) {
    }

    auxv_t* auxvp = (auxv_t*)++env;
    perform_self_relocations(auxvp);
    init_libc();

    FlatPtr entry = loader_main(auxvp);
    VERBOSE("Loaded libs:\n");
    for (auto& obj : g_loaded_objects) {
        (void)obj;
        VERBOSE("%s: %p\n", obj.key.characters(), obj.value->base_address().as_ptr());
    }

    MainFunction main_function = (MainFunction)(entry);
    VERBOSE("jumping to main program entry point: %p", main_function);
    int rc = main_function(argc, argv, envp);
    VERBOSE("rc: %d", rc);
    _exit(rc);
}
}
