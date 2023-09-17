/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <Kernel/API/POSIX/sys/stat.h>
#include <Kernel/API/VirtualMemoryAnnotations.h>
#include <LibCore/ArgsParser.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/Relocation.h>
#include <fcntl.h>
#include <sys/internals.h>
#include <syscall.h>
#include <unistd.h>

char* __static_environ[] = { nullptr }; // We don't get the environment without some libc workarounds..
char** environ = __static_environ;
uintptr_t __stack_chk_guard = 0;

extern "C" {

[[noreturn]] void _invoke_entry(int argc, char** argv, char** envp, ELF::EntryPointFunction entry);

static ErrorOr<int> open_executable(StringView path)
{
    int rc = open(path.characters_without_null_termination(), O_RDONLY | O_EXEC);
    if (rc < 0)
        return Error::from_errno(errno);
    int checked_fd = rc;
    ArmedScopeGuard close_on_failure([checked_fd] {
        close(checked_fd);
    });

    struct stat executable_stat = {};
    rc = fstat(checked_fd, &executable_stat);
    if (rc < 0)
        return Error::from_errno(errno);
    if (!S_ISREG(executable_stat.st_mode)) {
        if (S_ISDIR(executable_stat.st_mode))
            return Error::from_errno(EISDIR);
        return Error::from_errno(EINVAL);
    }

    close_on_failure.disarm();
    return checked_fd;
}

static int print_loaded_libraries_callback(struct dl_phdr_info* info, size_t, void*)
{
    outln("{}", info->dlpi_name);
    return 0;
}

static int _main(int argc, char** argv, char** envp, bool is_secure)
{
    Vector<StringView> arguments;
    arguments.ensure_capacity(argc);
    for (int i = 0; i < argc; ++i)
        arguments.unchecked_append({ argv[i], strlen(argv[i]) });

    bool flag_dry_run { false };
    bool flag_list_loaded_dependencies { false };
    Vector<StringView> command;
    StringView argv0;
    Core::ArgsParser args_parser;

    args_parser.set_general_help("Run dynamically-linked ELF executables");
    args_parser.set_stop_on_first_non_option(true);

    if (LexicalPath::basename(arguments[0]) == "ldd"sv) {
        flag_list_loaded_dependencies = true;
        flag_dry_run = true;
    } else {
        args_parser.add_option(flag_dry_run, "Run in dry-run mode", "dry-run", 'd');
        args_parser.add_option(flag_list_loaded_dependencies, "List all loaded dependencies", "list", 'l');
        args_parser.add_option(argv0, "Run with custom argv0", "argv0", 'E', "custom argv0");
    }
    args_parser.add_positional_argument(command, "Command to execute", "command");
    // NOTE: Don't use regular PrintUsageAndExit policy for ArgsParser, as it will simply
    // fail with a nullptr-dereference as the LibC exit function is not suitable for usage
    // in this piece of code.
    if (!args_parser.parse(arguments.span(), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (command.is_empty()) {
        args_parser.print_usage(stderr, arguments[0]);
        return 1;
    }

    auto error_or_fd = open_executable(command[0]);
    if (error_or_fd.is_error()) {
        warnln("Loader.so: Loading {} failed: {}", command[0], strerror(error_or_fd.error().code()));
        return 1;
    }

    int main_program_fd = error_or_fd.release_value();
    ByteString main_program_path = command[0];

    // NOTE: We need to extract the command with its arguments to be able
    // to run the actual requested executable with the requested parameters
    // from argv.
    VERIFY(command.size() <= static_cast<size_t>(argc));
    auto command_with_args = command.span();
    for (size_t index = 0; index < command.size(); index++)
        argv[index] = const_cast<char*>(command_with_args[index].characters_without_null_termination());

    if (!argv0.is_empty())
        argv[0] = const_cast<char*>(argv0.characters_without_null_termination());

    auto entry_point = ELF::DynamicLinker::linker_main(move(main_program_path), main_program_fd, is_secure, envp);
    if (flag_list_loaded_dependencies)
        ELF::DynamicLinker::iterate_over_loaded_shared_objects(print_loaded_libraries_callback, nullptr);
    if (flag_dry_run)
        return 0;
    _invoke_entry(command.size(), argv, envp, entry_point);
    VERIFY_NOT_REACHED();
}

// The compiler expects a previous declaration
void _start(int, char**, char**) __attribute__((used));
void _entry(int, char**, char**) __attribute__((used));

NAKED void _start(int, char**, char**)
{
#if ARCH(AARCH64)
    // Make sure backtrace computation stops here by setting FP and LR to 0.
    // FIXME: The kernel should ensure that registers are zeroed on program start
    asm(
        "mov x29, 0\n"
        "mov x30, 0\n"
        "bl _entry\n");
#elif ARCH(RISCV64)
    asm(
        "li fp, 0\n"
        "li ra, 0\n"
        "tail _entry@plt\n");
#elif ARCH(X86_64)
    asm(
        "push $0\n"
        "jmp _entry@plt\n");
#else
#    error "Unknown architecture"
#endif
}

ALWAYS_INLINE static void optimizer_fence()
{
    asm("" ::: "memory");
}

[[gnu::no_stack_protector]] void _entry(int argc, char** argv, char** envp)
{
    char** env;
    for (env = envp; *env; ++env) {
    }

    auxv_t* auxvp = (auxv_t*)++env;

    bool at_random_found = false;
    FlatPtr base_address = 0;
    bool base_address_found = false;

    for (auxv_t* entry = auxvp; entry->a_type != AT_NULL; ++entry) {
        if (entry->a_type == ELF::AuxiliaryValue::Random) {
            at_random_found = true;
            __stack_chk_guard = *reinterpret_cast<u64*>(entry->a_un.a_ptr);
        } else if (entry->a_type == ELF::AuxiliaryValue::BaseAddress) {
            base_address_found = true;
            base_address = entry->a_un.a_val;
        }
    }
    VERIFY(at_random_found && base_address_found);

    // Make sure compiler won't move any functions calls above __stack_chk_guard initialization even
    // if their definitions somehow become available.
    optimizer_fence();

    // We need to relocate ourselves.
    // (these relocations seem to be generated because of our vtables)
    if (!ELF::perform_relative_relocations(base_address)) {
        syscall(SC_dbgputstr, "Unable to perform relative relocations!\n", 40);
        VERIFY_NOT_REACHED();
    }

    // Similarly, make sure no non-offset-agnostic language features are used above this point.
    optimizer_fence();

    // Initialize the copy of libc included statically in Loader.so,
    // initialization of the dynamic libc.so is done by the DynamicLinker
    __libc_init();

    int main_program_fd = -1;
    ByteString main_program_path;
    bool is_secure = false;
    for (; auxvp->a_type != AT_NULL; ++auxvp) {
        if (auxvp->a_type == ELF::AuxiliaryValue::ExecFileDescriptor) {
            main_program_fd = auxvp->a_un.a_val;
        }
        if (auxvp->a_type == ELF::AuxiliaryValue::ExecFilename) {
            main_program_path = (char const*)auxvp->a_un.a_ptr;
        }
        if (auxvp->a_type == ELF::AuxiliaryValue::Secure) {
            is_secure = auxvp->a_un.a_val == 1;
        }
    }

    if (main_program_fd == -1) {
        // Allow syscalls from our code since the kernel won't do that automatically for us if we
        // were invoked directly.
        Elf_Ehdr* header = reinterpret_cast<Elf_Ehdr*>(base_address);
        Elf_Phdr* pheader = reinterpret_cast<Elf_Phdr*>(base_address + header->e_phoff);

        for (size_t i = 0; i < header->e_phnum; ++i) {
            auto const& segment = pheader[i];
            if (segment.p_type == PT_LOAD && (segment.p_flags & PF_X)) {
                auto flags = VirtualMemoryRangeFlags::SyscallCode | VirtualMemoryRangeFlags::Immutable;
                auto rc = syscall(Syscall::SC_annotate_mapping, segment.p_vaddr + base_address, flags);
                VERIFY(rc == 0);
            }
        }

        // We've been invoked directly as an executable rather than as the
        // ELF interpreter for some other binary.
        int exit_status = _main(argc, argv, envp, is_secure);
        _exit(exit_status);
    }

    VERIFY(main_program_fd >= 0);
    VERIFY(!main_program_path.is_empty());

    auto entry_point = ELF::DynamicLinker::linker_main(move(main_program_path), main_program_fd, is_secure, envp);
    _invoke_entry(argc, argv, envp, entry_point);
    VERIFY_NOT_REACHED();
}
}
