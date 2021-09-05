/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibC/sys/internals.h>
#include <LibC/unistd.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/DynamicLinker.h>
#include <LibELF/Relocation.h>

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
    const char message[] =
        R"(You have invoked `Loader.so'. This is the helper program for programs that
use shared libraries. Special directives embedded in executables tell the
kernel to load this program.

This helper program loads the shared libraries needed by the program,
prepares the program to run, and runs it. You do not need to invoke
this helper program directly.
)";
    fprintf(stderr, "%s", message);
}

extern "C" {

// The compiler expects a previous declaration
void _start(int, char**, char**) __attribute__((used));
void _entry(int, char**, char**) __attribute__((used));

NAKED void _start(int, char**, char**)
{
    asm(
        "push $0\n"
        "jmp _entry@plt\n");
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
    String main_program_name;
    bool is_secure = false;
    for (; auxvp->a_type != AT_NULL; ++auxvp) {
        if (auxvp->a_type == ELF::AuxiliaryValue::ExecFileDescriptor) {
            main_program_fd = auxvp->a_un.a_val;
        }
        if (auxvp->a_type == ELF::AuxiliaryValue::ExecFilename) {
            main_program_name = (const char*)auxvp->a_un.a_ptr;
        }
        if (auxvp->a_type == ELF::AuxiliaryValue::Secure) {
            is_secure = auxvp->a_un.a_val == 1;
        }
    }

    if (main_program_name == "/usr/lib/Loader.so"sv) {
        // We've been invoked directly as an executable rather than as the
        // ELF interpreter for some other binary. In the future we may want
        // to support launching a program directly from the dynamic loader
        // like ld.so on Linux.
        display_help();
        _exit(1);
    }

    VERIFY(main_program_fd >= 0);
    VERIFY(!main_program_name.is_empty());

    ELF::DynamicLinker::linker_main(move(main_program_name), main_program_fd, is_secure, argc, argv, envp);
    VERIFY_NOT_REACHED();
}
}
