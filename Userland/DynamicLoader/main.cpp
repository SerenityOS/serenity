/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <Kernel/API/POSIX/sys/stat.h>
#include <LibCore/ArgsParser.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/Relocation.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <sys/internals.h>
#include <syscall.h>
#include <unistd.h>

char* __static_environ[] = { nullptr }; // We don't get the environment without some libc workarounds..

static void init_libc()
{
    environ = __static_environ;
    __environ_is_malloced = false;
    __stdio_is_initialized = false;
    // Initialise the copy of libc included statically in Loader.so,
    // initialisation of the dynamic libc.so is done by the DynamicLinker
    __libc_init();
}

static void perform_self_relocations(auxv_t* auxvp)
{
    // We need to relocate ourselves.
    // (these relocations seem to be generated because of our vtables)

    FlatPtr base_address = 0;
    bool found_base_address = false;
    for (; auxvp->a_type != AT_NULL; ++auxvp) {
        if (auxvp->a_type == ELF::AuxiliaryValue::BaseAddress) {
            base_address = auxvp->a_un.a_val;
            found_base_address = true;
        }
    }
    VERIFY(found_base_address);
    if (!ELF::perform_relative_relocations(base_address))
        exit(1);
}

static ErrorOr<int> open_executable(StringView path)
{
    int rc = open(path.characters_without_null_termination(), O_RDONLY | O_EXEC, 0666);
    if (rc < 0)
        return Error::from_errno(errno);
    int checked_fd = rc;
    ArmedScopeGuard close_on_failure([checked_fd] {
        close(checked_fd);
    });

    struct stat executable_stat;
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

int main(int argc, char** argv, char** envp)
{
    tzset();

    Vector<StringView> arguments;
    arguments.ensure_capacity(argc);
    for (int i = 0; i < argc; ++i)
        arguments.unchecked_append({ argv[i], strlen(argv[i]) });

    bool flag_secure { false };
    bool flag_dry_run { false };
    Core::ArgsParser args_parser;
    Vector<StringView> command;
    StringView argv0;

    args_parser.set_general_help("Run dynamically-linked ELF executables");
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_option(flag_secure, "Run in secure-mode", "secure", 's');
    args_parser.add_option(flag_dry_run, "Run in dry-run mode", "dry-run", 'd');
    args_parser.add_option(argv0, "Run with custom argv0", "argv0", 'E', "custom argv0");
    args_parser.add_positional_argument(command, "Command to execute", "command");
    args_parser.parse(arguments.span());

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
    DeprecatedString main_program_path = command[0];

    // NOTE: We need to extract the command with its arguments to be able
    // to run the actual requested executable with the requested parameters
    // from argv.
    VERIFY(command.size() <= static_cast<size_t>(argc));
    auto command_with_args = command.span();
    for (size_t index = 0; index < command.size(); index++)
        argv[index] = const_cast<char*>(command_with_args[index].characters_without_null_termination());

    if (!argv0.is_empty())
        argv[0] = const_cast<char*>(argv0.characters_without_null_termination());

    ELF::DynamicLinker::linker_main(move(main_program_path), main_program_fd, flag_secure, flag_dry_run, command.size(), argv, envp);
    VERIFY_NOT_REACHED();
}

extern "C" {

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
#else
    asm(
        "push $0\n"
        "jmp _entry@plt\n");
#endif
}

void _entry(int argc, char** argv, char** envp)
{
    char** env;
    for (env = envp; *env; ++env) {
    }

    auxv_t* auxvp = (auxv_t*)++env;
    perform_self_relocations(auxvp);
    init_libc();

    int main_program_fd = -1;
    DeprecatedString main_program_path;
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

    if (main_program_path == "/usr/lib/Loader.so"sv) {
        // We've been invoked directly as an executable rather than as the
        // ELF interpreter for some other binary.
        int exit_status = main(argc, argv, envp);
        _exit(exit_status);
    }

    VERIFY(main_program_fd >= 0);
    VERIFY(!main_program_path.is_empty());

    ELF::DynamicLinker::linker_main(move(main_program_path), main_program_fd, is_secure, false, argc, argv, envp);
    VERIFY_NOT_REACHED();
}
}
