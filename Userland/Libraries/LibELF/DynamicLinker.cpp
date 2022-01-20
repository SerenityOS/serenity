/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2022, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/ScopeGuard.h>
#include <AK/Vector.h>
#include <LibC/bits/pthread_integration.h>
#include <LibC/link.h>
#include <LibC/sys/mman.h>
#include <LibC/unistd.h>
#include <LibDl/dlfcn.h>
#include <LibDl/dlfcn_integration.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Hashes.h>
#include <fcntl.h>
#include <string.h>
#include <sys/types.h>
#include <syscall.h>

namespace ELF {

static HashMap<String, NonnullRefPtr<ELF::DynamicLoader>> s_loaders;
static String s_main_program_name;
static OrderedHashMap<String, NonnullRefPtr<ELF::DynamicObject>> s_global_objects;

using EntryPointFunction = int (*)(int, char**, char**);
using LibCExitFunction = void (*)(int);
using DlIteratePhdrCallbackFunction = int (*)(struct dl_phdr_info*, size_t, void*);
using DlIteratePhdrFunction = int (*)(DlIteratePhdrCallbackFunction, void*);

extern "C" [[noreturn]] void _invoke_entry(int argc, char** argv, char** envp, EntryPointFunction entry);

static size_t s_current_tls_offset = 0;
static size_t s_total_tls_size = 0;
static size_t s_allocated_tls_block_size = 0;
static char** s_envp = nullptr;
static LibCExitFunction s_libc_exit = nullptr;
static __pthread_mutex_t s_loader_lock = __PTHREAD_MUTEX_INITIALIZER;

static bool s_allowed_to_check_environment_variables { false };
static bool s_do_breakpoint_trap_before_entry { false };
static StringView s_ld_library_path;

static Result<void, DlErrorMessage> __dlclose(void* handle);
static Result<void*, DlErrorMessage> __dlopen(const char* filename, int flags);
static Result<void*, DlErrorMessage> __dlsym(void* handle, const char* symbol_name);
static Result<void, DlErrorMessage> __dladdr(void* addr, Dl_info* info);

Optional<DynamicObject::SymbolLookupResult> DynamicLinker::lookup_global_symbol(StringView name)
{
    Optional<DynamicObject::SymbolLookupResult> weak_result;

    auto symbol = DynamicObject::HashSymbol { name };

    for (auto& lib : s_global_objects) {
        auto res = lib.value->lookup_symbol(symbol);
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

static String get_library_name(String path)
{
    return LexicalPath::basename(move(path));
}

static Result<NonnullRefPtr<DynamicLoader>, DlErrorMessage> map_library(const String& filename, int fd)
{
    auto result = ELF::DynamicLoader::try_create(fd, filename);
    if (result.is_error()) {
        return result;
    }

    auto& loader = result.value();

    s_loaders.set(get_library_name(filename), *loader);

    s_current_tls_offset -= loader->tls_size_of_current_object();
    loader->set_tls_offset(s_current_tls_offset);

    return loader;
}

static Result<NonnullRefPtr<DynamicLoader>, DlErrorMessage> map_library(const String& name)
{
    if (name.contains("/"sv)) {
        int fd = open(name.characters(), O_RDONLY);
        if (fd < 0)
            return DlErrorMessage { String::formatted("Could not open shared library: {}", name) };
        return map_library(name, fd);
    }

    // Scan the LD_LIBRARY_PATH environment variable if applicable
    if (s_ld_library_path != nullptr) {
        for (const auto& search_path : s_ld_library_path.split_view(':')) {
            LexicalPath library_path(search_path);
            int fd = open(library_path.append(name).string().characters(), O_RDONLY);
            if (fd < 0)
                continue;
            return map_library(name, fd);
        }
    }

    // Now check the default paths.
    // TODO: Do we want to also look for libs in other paths too?
    const char* search_paths[] = { "/usr/lib/{}", "/usr/local/lib/{}" };
    for (auto& search_path : search_paths) {
        auto path = String::formatted(search_path, name);
        int fd = open(path.characters(), O_RDONLY);
        if (fd < 0)
            continue;
        return map_library(name, fd);
    }

    return DlErrorMessage { String::formatted("Could not find required shared library: {}", name) };
}

static Vector<String> get_dependencies(const String& name)
{
    auto lib = s_loaders.get(name).value();
    Vector<String> dependencies;

    lib->for_each_needed_library([&dependencies, &name](auto needed_name) {
        if (name == needed_name)
            return;
        dependencies.append(needed_name);
    });
    return dependencies;
}

static Result<void, DlErrorMessage> map_dependencies(const String& name)
{
    dbgln_if(DYNAMIC_LOAD_DEBUG, "mapping dependencies for: {}", name);

    for (const auto& needed_name : get_dependencies(name)) {
        dbgln_if(DYNAMIC_LOAD_DEBUG, "needed library: {}", needed_name.characters());
        String library_name = get_library_name(needed_name);

        if (!s_loaders.contains(library_name) && !s_global_objects.contains(library_name)) {
            auto result1 = map_library(needed_name);
            if (result1.is_error()) {
                return result1.error();
            }
            auto result2 = map_dependencies(library_name);
            if (result2.is_error()) {
                return result2.error();
            }
        }
    }
    dbgln_if(DYNAMIC_LOAD_DEBUG, "mapped dependencies for {}", name);
    return {};
}

static void allocate_tls()
{
    s_total_tls_size = 0;
    for (const auto& data : s_loaders) {
        dbgln_if(DYNAMIC_LOAD_DEBUG, "{}: TLS Size: {}", data.key, data.value->tls_size_of_current_object());
        s_total_tls_size += data.value->tls_size_of_current_object();
    }

    if (!s_total_tls_size)
        return;

    auto page_aligned_size = align_up_to(s_total_tls_size, PAGE_SIZE);
    auto initial_tls_data_result = ByteBuffer::create_zeroed(page_aligned_size);
    if (initial_tls_data_result.is_error()) {
        dbgln("Failed to allocate initial TLS data");
        VERIFY_NOT_REACHED();
    }

    auto& initial_tls_data = initial_tls_data_result.value();

    // Initialize TLS data
    for (const auto& entry : s_loaders) {
        entry.value->copy_initial_tls_data_into(initial_tls_data);
    }

    void* master_tls = ::allocate_tls((char*)initial_tls_data.data(), initial_tls_data.size());
    VERIFY(master_tls != (void*)-1);
    dbgln_if(DYNAMIC_LOAD_DEBUG, "from userspace, master_tls: {:p}", master_tls);

    s_allocated_tls_block_size = initial_tls_data.size();
}

static int __dl_iterate_phdr(DlIteratePhdrCallbackFunction callback, void* data)
{
    __pthread_mutex_lock(&s_loader_lock);
    ScopeGuard unlock_guard = [] { __pthread_mutex_unlock(&s_loader_lock); };

    for (auto& it : s_global_objects) {
        auto& object = it.value;
        auto info = dl_phdr_info {
            .dlpi_addr = (ElfW(Addr))object->base_address().as_ptr(),
            .dlpi_name = object->filename().characters(),
            .dlpi_phdr = object->program_headers(),
            .dlpi_phnum = object->program_header_count()
        };

        auto res = callback(&info, sizeof(info), data);
        if (res != 0)
            return res;
    }

    return 0;
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
    *((char***)res.value().address.as_ptr()) = s_envp;

    res = libc.lookup_symbol("__environ_is_malloced"sv);
    VERIFY(res.has_value());
    *((bool*)res.value().address.as_ptr()) = false;

    res = libc.lookup_symbol("exit"sv);
    VERIFY(res.has_value());
    s_libc_exit = (LibCExitFunction)res.value().address.as_ptr();

    res = libc.lookup_symbol("__dl_iterate_phdr"sv);
    VERIFY(res.has_value());
    *((DlIteratePhdrFunction*)res.value().address.as_ptr()) = __dl_iterate_phdr;

    res = libc.lookup_symbol("__dlclose"sv);
    VERIFY(res.has_value());
    *((DlCloseFunction*)res.value().address.as_ptr()) = __dlclose;

    res = libc.lookup_symbol("__dlopen"sv);
    VERIFY(res.has_value());
    *((DlOpenFunction*)res.value().address.as_ptr()) = __dlopen;

    res = libc.lookup_symbol("__dlsym"sv);
    VERIFY(res.has_value());
    *((DlSymFunction*)res.value().address.as_ptr()) = __dlsym;

    res = libc.lookup_symbol("__dladdr"sv);
    VERIFY(res.has_value());
    *((DlAddrFunction*)res.value().address.as_ptr()) = __dladdr;

    res = libc.lookup_symbol("__libc_init"sv);
    VERIFY(res.has_value());
    typedef void libc_init_func();
    ((libc_init_func*)res.value().address.as_ptr())();
}

template<typename Callback>
static void for_each_unfinished_dependency_of(const String& name, HashTable<String>& seen_names, bool first, bool skip_global_objects, Callback callback)
{
    if (!s_loaders.contains(name))
        return;

    if (!first && skip_global_objects && s_global_objects.contains(name))
        return;

    if (seen_names.contains(name))
        return;
    seen_names.set(name);

    for (const auto& needed_name : get_dependencies(name))
        for_each_unfinished_dependency_of(get_library_name(needed_name), seen_names, false, skip_global_objects, callback);

    callback(*s_loaders.get(name).value());
}

static NonnullRefPtrVector<DynamicLoader> collect_loaders_for_library(const String& name, bool skip_global_objects)
{
    HashTable<String> seen_names;
    NonnullRefPtrVector<DynamicLoader> loaders;
    for_each_unfinished_dependency_of(name, seen_names, true, skip_global_objects, [&](auto& loader) {
        loaders.append(loader);
    });
    return loaders;
}

static Result<NonnullRefPtr<DynamicLoader>, DlErrorMessage> load_main_library(const String& name, int flags, bool skip_global_objects)
{
    auto main_library_loader = *s_loaders.get(name);
    auto main_library_object = main_library_loader->map();
    s_global_objects.set(name, *main_library_object);

    auto loaders = collect_loaders_for_library(name, skip_global_objects);

    for (auto& loader : loaders) {
        auto dynamic_object = loader.map();
        if (dynamic_object)
            s_global_objects.set(dynamic_object->filename(), *dynamic_object);
    }

    for (auto& loader : loaders) {
        bool success = loader.link(flags);
        if (!success) {
            return DlErrorMessage { String::formatted("Failed to link library {}", loader.filename()) };
        }
    }

    for (auto& loader : loaders) {
        auto result = loader.load_stage_3(flags);
        VERIFY(!result.is_error());
        auto& object = result.value();

        if (loader.filename() == "libsystem.so"sv) {
            VERIFY(!loader.text_segments().is_empty());
            for (const auto& segment : loader.text_segments()) {
                if (syscall(SC_msyscall, segment.address().get())) {
                    VERIFY_NOT_REACHED();
                }
            }
        }

        if (loader.filename() == "libc.so"sv) {
            initialize_libc(*object);
        }
    }

    for (auto& loader : loaders) {
        loader.load_stage_4();
    }

    return NonnullRefPtr<DynamicLoader>(*main_library_loader);
}

static Result<void, DlErrorMessage> __dlclose(void* handle)
{
    dbgln_if(DYNAMIC_LOAD_DEBUG, "__dlclose: {}", handle);

    __pthread_mutex_lock(&s_loader_lock);
    ScopeGuard unlock_guard = [] { __pthread_mutex_unlock(&s_loader_lock); };

    // FIXME: this will not currently destroy the dynamic object
    // because we're intentionally holding a strong reference to it
    // via s_global_objects until there's proper unload support.
    auto object = static_cast<ELF::DynamicObject*>(handle);
    object->unref();
    return {};
}

static Optional<DlErrorMessage> verify_tls_for_dlopen(const DynamicLoader& loader)
{
    if (loader.tls_size_of_current_object() == 0)
        return {};

    if (s_total_tls_size + loader.tls_size_of_current_object() > s_allocated_tls_block_size)
        return DlErrorMessage("TLS size too large");

    bool tls_data_is_all_zero = true;
    loader.image().for_each_program_header([&loader, &tls_data_is_all_zero](ELF::Image::ProgramHeader program_header) {
        if (program_header.type() != PT_TLS)
            return IterationDecision::Continue;

        auto* tls_data = (const u8*)loader.image().base_address() + program_header.offset();
        for (size_t i = 0; i < program_header.size_in_image(); ++i) {
            if (tls_data[i] != 0) {
                tls_data_is_all_zero = false;
                break;
            }
        }
        return IterationDecision::Break;
    });

    if (tls_data_is_all_zero)
        return {};

    return DlErrorMessage("Using dlopen() with libraries that have non-zeroed TLS is currently not supported");
}

static Result<void*, DlErrorMessage> __dlopen(const char* filename, int flags)
{
    // FIXME: RTLD_NOW and RTLD_LOCAL are not supported
    flags &= ~RTLD_NOW;
    flags |= RTLD_LAZY;
    flags &= ~RTLD_LOCAL;
    flags |= RTLD_GLOBAL;

    dbgln_if(DYNAMIC_LOAD_DEBUG, "__dlopen invoked, filename={}, flags={}", filename, flags);

    auto library_name = get_library_name(filename ? filename : s_main_program_name);

    if (__pthread_mutex_trylock(&s_loader_lock) != 0)
        return DlErrorMessage { "Nested calls to dlopen() are not permitted." };
    ScopeGuard unlock_guard = [] { __pthread_mutex_unlock(&s_loader_lock); };

    auto existing_elf_object = s_global_objects.get(library_name);
    if (existing_elf_object.has_value()) {
        // It's up to the caller to release the ref with dlclose().
        existing_elf_object.value()->ref();
        return *existing_elf_object;
    }

    VERIFY(!library_name.is_empty());

    auto result1 = map_library(filename);
    if (result1.is_error()) {
        return result1.error();
    }

    if (auto error = verify_tls_for_dlopen(result1.value()); error.has_value())
        return error.value();

    auto result2 = map_dependencies(library_name);
    if (result2.is_error()) {
        return result2.error();
    }

    auto result = load_main_library(library_name, flags, true);
    if (result.is_error())
        return result.error();

    s_total_tls_size += result1.value()->tls_size_of_current_object();

    auto object = s_global_objects.get(library_name);
    if (!object.has_value())
        return DlErrorMessage { "Could not load ELF object." };

    // It's up to the caller to release the ref with dlclose().
    object.value()->ref();
    return *object;
}

static Result<void*, DlErrorMessage> __dlsym(void* handle, const char* symbol_name)
{
    dbgln_if(DYNAMIC_LOAD_DEBUG, "__dlsym: {}, {}", handle, symbol_name);

    __pthread_mutex_lock(&s_loader_lock);
    ScopeGuard unlock_guard = [] { __pthread_mutex_unlock(&s_loader_lock); };

    auto object = static_cast<DynamicObject*>(handle);
    if (!handle) {
        auto library_name = get_library_name(s_main_program_name);
        auto global_object = s_global_objects.get(library_name);
        object = *global_object;
    }
    auto symbol = object->lookup_symbol(symbol_name);
    if (!symbol.has_value()) {
        return DlErrorMessage { String::formatted("Symbol {} not found", symbol_name) };
    }
    return symbol.value().address.as_ptr();
}

static Result<void, DlErrorMessage> __dladdr(void* addr, Dl_info* info)
{
    VirtualAddress user_addr { addr };
    __pthread_mutex_lock(&s_loader_lock);
    ScopeGuard unlock_guard = [] { __pthread_mutex_unlock(&s_loader_lock); };

    RefPtr<DynamicObject> best_matching_library;
    VirtualAddress best_library_offset;
    for (auto& lib : s_global_objects) {
        if (user_addr < lib.value->base_address())
            continue;
        auto offset = user_addr - lib.value->base_address();
        if (!best_matching_library || offset < best_library_offset) {
            best_matching_library = lib.value;
            best_library_offset = offset;
        }
    }

    if (!best_matching_library) {
        return DlErrorMessage { "No library found which contains the specified address" };
    }

    Optional<DynamicObject::Symbol> best_matching_symbol;
    best_matching_library->for_each_symbol([&](auto const& symbol) {
        if (user_addr < symbol.address() || user_addr > symbol.address().offset(symbol.size()))
            return;
        best_matching_symbol = symbol;
    });

    info->dli_fbase = best_matching_library->base_address().as_ptr();
    // This works because we don't support unloading objects.
    info->dli_fname = best_matching_library->filename().characters();
    if (best_matching_symbol.has_value()) {
        info->dli_saddr = best_matching_symbol.value().address().as_ptr();
        info->dli_sname = best_matching_symbol.value().raw_name();
    } else {
        info->dli_saddr = nullptr;
        info->dli_sname = nullptr;
    }
    return {};
}

static void read_environment_variables()
{
    for (char** env = s_envp; *env; ++env) {
        StringView env_string { *env };
        if (env_string == "_LOADER_BREAKPOINT=1"sv) {
            s_do_breakpoint_trap_before_entry = true;
        }

        constexpr auto library_path_string = "LD_LIBRARY_PATH="sv;
        if (env_string.starts_with(library_path_string)) {
            s_ld_library_path = env_string.substring_view(library_path_string.length());
        }
    }
}

void ELF::DynamicLinker::linker_main(String&& main_program_name, int main_program_fd, bool is_secure, int argc, char** argv, char** envp)
{
    s_envp = envp;

    s_allowed_to_check_environment_variables = !is_secure;
    if (s_allowed_to_check_environment_variables)
        read_environment_variables();

    s_main_program_name = main_program_name;

    auto library_name = get_library_name(main_program_name);

    // NOTE: We always map the main library first, since it may require
    //       placement at a specific address.
    auto result1 = map_library(main_program_name, main_program_fd);
    if (result1.is_error()) {
        warnln("{}", result1.error().text);
        fflush(stderr);
        _exit(1);
    }
    (void)result1.release_value();

    auto result2 = map_dependencies(library_name);
    if (result2.is_error()) {
        warnln("{}", result2.error().text);
        fflush(stderr);
        _exit(1);
    }

    dbgln_if(DYNAMIC_LOAD_DEBUG, "loaded all dependencies");
    for ([[maybe_unused]] auto& lib : s_loaders) {
        dbgln_if(DYNAMIC_LOAD_DEBUG, "{} - tls size: {}, tls offset: {}", lib.key, lib.value->tls_size_of_current_object(), lib.value->tls_offset());
    }

    allocate_tls();

    auto entry_point_function = [&main_program_name] {
        auto library_name = get_library_name(main_program_name);
        auto result = load_main_library(library_name, RTLD_GLOBAL | RTLD_LAZY, false);
        if (result.is_error()) {
            warnln("{}", result.error().text);
            _exit(1);
        }
        auto& main_executable_loader = result.value();
        auto entry_point = main_executable_loader->image().entry();
        if (main_executable_loader->is_dynamic())
            entry_point = entry_point.offset(main_executable_loader->base_address().get());
        return (EntryPointFunction)(entry_point.as_ptr());
    }();

    s_loaders.clear();

    int rc = syscall(SC_msyscall, nullptr);
    if (rc < 0) {
        VERIFY_NOT_REACHED();
    }

    dbgln_if(DYNAMIC_LOAD_DEBUG, "Jumping to entry point: {:p}", entry_point_function);
    if (s_do_breakpoint_trap_before_entry) {
        asm("int3");
    }

    _invoke_entry(argc, argv, envp, entry_point_function);
    VERIFY_NOT_REACHED();
}

}
