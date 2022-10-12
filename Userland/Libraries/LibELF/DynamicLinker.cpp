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
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Hashes.h>
#include <bits/dlfcn_integration.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <pthread.h>
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
static StringView s_main_program_pledge_promises;
static String s_loader_pledge_promises;

static Result<void, DlErrorMessage> __dlclose(void* handle);
static Result<void*, DlErrorMessage> __dlopen(char const* filename, int flags);
static Result<void*, DlErrorMessage> __dlsym(void* handle, char const* symbol_name);
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

static Result<NonnullRefPtr<DynamicLoader>, DlErrorMessage> map_library(String const& filename, int fd, String const& filepath)
{
    auto result = ELF::DynamicLoader::try_create(fd, filename, filepath);
    if (result.is_error()) {
        return result;
    }

    auto& loader = result.value();

    s_loaders.set(get_library_name(filename), *loader);

    s_current_tls_offset -= loader->tls_size_of_current_object();
    if (loader->tls_alignment_of_current_object())
        s_current_tls_offset = align_down_to(s_current_tls_offset, loader->tls_alignment_of_current_object());
    loader->set_tls_offset(s_current_tls_offset);

    // This actually maps the library at the intended and final place.
    auto main_library_object = loader->map();
    s_global_objects.set(get_library_name(filename), *main_library_object);

    return loader;
}

static Optional<String> resolve_library(String const& name, DynamicObject const& parent_object)
{
    Vector<StringView> search_paths;

    // Search RPATH values indicated by the ELF (only if RUNPATH is not present).
    if (parent_object.runpath().is_empty())
        search_paths.extend(parent_object.rpath().split_view(':'));

    // Scan the LD_LIBRARY_PATH environment variable if applicable.
    search_paths.extend(s_ld_library_path.split_view(':'));

    // Search RUNPATH values indicated by the ELF.
    search_paths.extend(parent_object.runpath().split_view(':'));

    // Last are the default search paths.
    search_paths.append("/usr/lib"sv);
    search_paths.append("/usr/local/lib"sv);

    for (auto const& search_path : search_paths) {
        LexicalPath library_path(search_path.replace("$ORIGIN"sv, LexicalPath::dirname(parent_object.filepath()), ReplaceMode::FirstOnly));
        String library_name = library_path.append(name).string();

        if (access(library_name.characters(), F_OK) == 0)
            return library_name;
    }

    return {};
}

static Result<NonnullRefPtr<DynamicLoader>, DlErrorMessage> map_library(String const& name, DynamicObject const& parent_object)
{
    if (name.contains("/"sv)) {
        int fd = open(name.characters(), O_RDONLY);
        if (fd < 0)
            return DlErrorMessage { String::formatted("Could not open shared library: {}", name) };
        return map_library(name, fd, name);
    }

    auto resolved_library_name = resolve_library(name, parent_object);
    if (!resolved_library_name.has_value())
        return DlErrorMessage { String::formatted("Could not find required shared library: {}", name) };

    int fd = open(resolved_library_name.value().characters(), O_RDONLY);
    if (fd < 0)
        return DlErrorMessage { String::formatted("Could not open resolved shared library '{}': {}", resolved_library_name.value(), strerror(errno)) };

    return map_library(name, fd, resolved_library_name.value());
}

static Vector<String> get_dependencies(String const& name)
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

static Result<void, DlErrorMessage> map_dependencies(String const& name)
{
    dbgln_if(DYNAMIC_LOAD_DEBUG, "mapping dependencies for: {}", name);

    auto const& parent_object = (*s_loaders.get(name))->dynamic_object();

    for (auto const& needed_name : get_dependencies(name)) {
        dbgln_if(DYNAMIC_LOAD_DEBUG, "needed library: {}", needed_name.characters());
        String library_name = get_library_name(needed_name);

        if (!s_loaders.contains(library_name) && !s_global_objects.contains(library_name)) {
            auto result1 = map_library(needed_name, parent_object);
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
    for (auto const& data : s_loaders) {
        dbgln_if(DYNAMIC_LOAD_DEBUG, "{}: TLS Size: {}, TLS Alignment: {}", data.key, data.value->tls_size_of_current_object(), data.value->tls_alignment_of_current_object());
        s_total_tls_size += data.value->tls_size_of_current_object() + data.value->tls_alignment_of_current_object();
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
    for (auto const& entry : s_loaders) {
        entry.value->copy_initial_tls_data_into(initial_tls_data);
    }

    void* master_tls = ::allocate_tls((char*)initial_tls_data.data(), initial_tls_data.size());
    VERIFY(master_tls != (void*)-1);
    dbgln_if(DYNAMIC_LOAD_DEBUG, "from userspace, master_tls: {:p}", master_tls);

    s_allocated_tls_block_size = initial_tls_data.size();
}

static int __dl_iterate_phdr(DlIteratePhdrCallbackFunction callback, void* data)
{
    pthread_mutex_lock(&s_loader_lock);
    ScopeGuard unlock_guard = [] { pthread_mutex_unlock(&s_loader_lock); };

    for (auto& it : s_global_objects) {
        auto& object = it.value;
        auto info = dl_phdr_info {
            .dlpi_addr = (ElfW(Addr))object->base_address().as_ptr(),
            .dlpi_name = object->filepath().characters(),
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

    // __stack_chk_guard should be initialized before anything significant (read: global constructors) is running.
    // This is not done in __libc_init, as we definitely have to return from that, and it might affect Loader as well.
    res = libc.lookup_symbol("__stack_chk_guard"sv);
    VERIFY(res.has_value());
    arc4random_buf(res.value().address.as_ptr(), sizeof(size_t));

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
static void for_each_unfinished_dependency_of(String const& name, HashTable<String>& seen_names, Callback callback)
{
    auto loader = s_loaders.get(name);

    if (!loader.has_value())
        return;

    if (loader.value()->is_fully_relocated()) {
        if (!loader.value()->is_fully_initialized()) {
            // If we are ending up here, that possibly means that this library either dlopens itself or a library that depends
            // on it while running its initializers. Assuming that this is the only funny thing that the library does, there is
            // a reasonable chance that nothing breaks, so just warn and continue.
            dbgln("\033[33mWarning:\033[0m Querying for dependencies of '{}' while running its initializers", name);
        }

        return;
    }

    if (seen_names.contains(name))
        return;
    seen_names.set(name);

    for (auto const& needed_name : get_dependencies(name))
        for_each_unfinished_dependency_of(get_library_name(needed_name), seen_names, callback);

    callback(*s_loaders.get(name).value());
}

static NonnullRefPtrVector<DynamicLoader> collect_loaders_for_library(String const& name)
{
    HashTable<String> seen_names;
    NonnullRefPtrVector<DynamicLoader> loaders;
    for_each_unfinished_dependency_of(name, seen_names, [&](auto& loader) {
        loaders.append(loader);
    });
    return loaders;
}

static void drop_loader_promise(StringView promise_to_drop)
{
    if (s_main_program_pledge_promises.is_empty() || s_loader_pledge_promises.is_empty())
        return;

    s_loader_pledge_promises = s_loader_pledge_promises.replace(promise_to_drop, ""sv, ReplaceMode::All);

    auto extended_promises = String::formatted("{} {}", s_main_program_pledge_promises, s_loader_pledge_promises);
    Syscall::SC_pledge_params params {
        { extended_promises.characters(), extended_promises.length() },
        { nullptr, 0 },
    };
    int rc = syscall(SC_pledge, &params);
    if (rc < 0 && rc > -EMAXERRNO) {
        warnln("Failed to drop loader pledge promise: {}. errno={}", promise_to_drop, errno);
        _exit(1);
    }
}

static Result<void, DlErrorMessage> link_main_library(String const& name, int flags)
{
    auto loaders = collect_loaders_for_library(name);

    for (auto& loader : loaders) {
        auto dynamic_object = loader.map();
        if (dynamic_object)
            s_global_objects.set(get_library_name(dynamic_object->filepath()), *dynamic_object);
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
            for (auto const& segment : loader.text_segments()) {
                if (syscall(SC_msyscall, segment.address().get())) {
                    VERIFY_NOT_REACHED();
                }
            }
        }

        if (loader.filename() == "libc.so"sv) {
            initialize_libc(*object);
        }
    }

    drop_loader_promise("prot_exec"sv);

    for (auto& loader : loaders) {
        loader.load_stage_4();
    }

    return {};
}

static Result<void, DlErrorMessage> __dlclose(void* handle)
{
    dbgln_if(DYNAMIC_LOAD_DEBUG, "__dlclose: {}", handle);

    pthread_mutex_lock(&s_loader_lock);
    ScopeGuard unlock_guard = [] { pthread_mutex_unlock(&s_loader_lock); };

    // FIXME: this will not currently destroy the dynamic object
    // because we're intentionally holding a strong reference to it
    // via s_global_objects until there's proper unload support.
    auto object = static_cast<ELF::DynamicObject*>(handle);
    object->unref();
    return {};
}

static Optional<DlErrorMessage> verify_tls_for_dlopen(DynamicLoader const& loader)
{
    if (loader.tls_size_of_current_object() == 0)
        return {};

    if (s_total_tls_size + loader.tls_size_of_current_object() + loader.tls_alignment_of_current_object() > s_allocated_tls_block_size)
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

static Result<void*, DlErrorMessage> __dlopen(char const* filename, int flags)
{
    // FIXME: RTLD_NOW and RTLD_LOCAL are not supported
    flags &= ~RTLD_NOW;
    flags |= RTLD_LAZY;
    flags &= ~RTLD_LOCAL;
    flags |= RTLD_GLOBAL;

    dbgln_if(DYNAMIC_LOAD_DEBUG, "__dlopen invoked, filename={}, flags={}", filename, flags);

    auto library_name = get_library_name(filename ? filename : s_main_program_name);

    if (pthread_mutex_trylock(&s_loader_lock) != 0)
        return DlErrorMessage { "Nested calls to dlopen() are not permitted." };
    ScopeGuard unlock_guard = [] { pthread_mutex_unlock(&s_loader_lock); };

    auto existing_elf_object = s_global_objects.get(library_name);
    if (existing_elf_object.has_value()) {
        // It's up to the caller to release the ref with dlclose().
        existing_elf_object.value()->ref();
        return *existing_elf_object;
    }

    VERIFY(!library_name.is_empty());

    auto const& parent_object = **s_global_objects.get(get_library_name(s_main_program_name));

    auto result1 = map_library(filename, parent_object);
    if (result1.is_error()) {
        return result1.error();
    }

    if (auto error = verify_tls_for_dlopen(result1.value()); error.has_value())
        return error.value();

    auto result2 = map_dependencies(library_name);
    if (result2.is_error()) {
        return result2.error();
    }

    auto result = link_main_library(library_name, flags);
    if (result.is_error())
        return result.error();

    s_total_tls_size += result1.value()->tls_size_of_current_object() + result1.value()->tls_alignment_of_current_object();

    auto object = s_global_objects.get(library_name);
    if (!object.has_value())
        return DlErrorMessage { "Could not load ELF object." };

    // It's up to the caller to release the ref with dlclose().
    object.value()->ref();
    return *object;
}

static Result<void*, DlErrorMessage> __dlsym(void* handle, char const* symbol_name)
{
    dbgln_if(DYNAMIC_LOAD_DEBUG, "__dlsym: {}, {}", handle, symbol_name);

    pthread_mutex_lock(&s_loader_lock);
    ScopeGuard unlock_guard = [] { pthread_mutex_unlock(&s_loader_lock); };

    StringView symbol_name_view { symbol_name, strlen(symbol_name) };
    Optional<DynamicObject::SymbolLookupResult> symbol;

    if (handle) {
        auto object = static_cast<DynamicObject*>(handle);
        symbol = object->lookup_symbol(symbol_name_view);
    } else {
        // When handle is 0 (RTLD_DEFAULT) we should look up the symbol in all global modules
        // https://pubs.opengroup.org/onlinepubs/009604499/functions/dlsym.html
        symbol = DynamicLinker::lookup_global_symbol(symbol_name_view);
    }

    if (!symbol.has_value())
        return DlErrorMessage { String::formatted("Symbol {} not found", symbol_name_view) };

    if (symbol.value().type == STT_GNU_IFUNC)
        return (void*)reinterpret_cast<DynamicObject::IfuncResolver>(symbol.value().address.as_ptr())();
    return symbol.value().address.as_ptr();
}

static Result<void, DlErrorMessage> __dladdr(void* addr, Dl_info* info)
{
    VirtualAddress user_addr { addr };
    pthread_mutex_lock(&s_loader_lock);
    ScopeGuard unlock_guard = [] { pthread_mutex_unlock(&s_loader_lock); };

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
    info->dli_fname = best_matching_library->filepath().characters();
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
        StringView env_string { *env, strlen(*env) };
        if (env_string == "_LOADER_BREAKPOINT=1"sv) {
            s_do_breakpoint_trap_before_entry = true;
        }

        constexpr auto library_path_string = "LD_LIBRARY_PATH="sv;
        if (env_string.starts_with(library_path_string)) {
            s_ld_library_path = env_string.substring_view(library_path_string.length());
        }

        constexpr auto main_pledge_promises_key = "_LOADER_MAIN_PROGRAM_PLEDGE_PROMISES="sv;
        if (env_string.starts_with(main_pledge_promises_key)) {
            s_main_program_pledge_promises = env_string.substring_view(main_pledge_promises_key.length());
        }

        constexpr auto loader_pledge_promises_key = "_LOADER_PLEDGE_PROMISES="sv;
        if (env_string.starts_with(loader_pledge_promises_key)) {
            s_loader_pledge_promises = env_string.substring_view(loader_pledge_promises_key.length());
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
    auto result1 = map_library(main_program_name, main_program_fd, main_program_name);
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
        dbgln_if(DYNAMIC_LOAD_DEBUG, "{} - tls size: {}, tls alignment: {}, tls offset: {}", lib.key, lib.value->tls_size_of_current_object(), lib.value->tls_alignment_of_current_object(), lib.value->tls_offset());
    }

    allocate_tls();

    auto entry_point_function = [&main_program_name] {
        auto library_name = get_library_name(main_program_name);
        auto result = link_main_library(library_name, RTLD_GLOBAL | RTLD_LAZY);
        if (result.is_error()) {
            warnln("{}", result.error().text);
            _exit(1);
        }

        drop_loader_promise("rpath"sv);

        auto& main_executable_loader = *s_loaders.get(library_name);
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
#ifdef AK_ARCH_AARCH64
        asm("brk #0");
#else
        asm("int3");
#endif
    }

    _invoke_entry(argc, argv, envp, entry_point_function);
    VERIFY_NOT_REACHED();
}

}
