/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <Kernel/API/POSIX/sys/stat.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/Relocation.h>
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

static void display_help()
{
    char const message[] =
        R"(You have invoked `Loader.so'. This is the helper program for programs that
use shared libraries. Special directives embedded in executables tell the
kernel to load this program.

This helper program loads the shared libraries needed by the program,
prepares the program to run, and runs it. You do not need to invoke
this helper program directly. If you still want to run a program with the loader,
run: /usr/lib/Loader.so [ELF_BINARY])";
    fprintf(stderr, "%s", message);
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

static ErrorOr<int> open_executable(char const* path)
{
    int rc = open(path, O_RDONLY | O_EXEC);
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

void _entry(int argc, char** argv, char** envp)
{
    char** env;
    for (env = envp; *env; ++env) {
    }

    auxv_t* auxvp = (auxv_t*)++env;
    perform_self_relocations(auxvp);
    init_libc();

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
        // We've been invoked directly as an executable rather than as the
        // ELF interpreter for some other binary. The second argv string should
        // be the path to the ELF executable, and if we don't have enough strings in argv
        // (argc < 2), just fail with message to stderr about this.

        if (argc < 2) {
            display_help();
            _exit(1);
        }
        auto error_or_fd = open_executable(argv[1]);
        if (error_or_fd.is_error()) {
            warnln("Loader.so: Loading {} failed: {}", argv[1], strerror(error_or_fd.error().code()));
            _exit(1);
        }
        main_program_fd = error_or_fd.release_value();
        main_program_path = argv[1];
        argv++;
        argc--;
    }

    VERIFY(main_program_fd >= 0);
    VERIFY(!main_program_path.is_empty());

    ELF::DynamicLinker::linker_main(move(main_program_path), main_program_fd, is_secure, argc, argv, envp);
    VERIFY_NOT_REACHED();
}
}
