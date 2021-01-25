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
#include <LibELF/DynamicLinker.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Image.h>
#include <LibELF/exec_elf.h>
#include <dlfcn.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>

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
void _start(int, char**, char**);

void _start(int argc, char** argv, char** envp)
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

    if (main_program_name == "/usr/lib/Loader.so") {
        // We've been invoked directly as an executable rather than as the
        // ELF interpreter for some other binary. In the future we may want
        // to support launching a program directly from the dynamic loader
        // like ld.so on Linux.
        display_help();
        _exit(1);
    }

    ASSERT(main_program_fd >= 0);
    ASSERT(!main_program_name.is_empty());

    ELF::DynamicLinker::linker_main(move(main_program_name), main_program_fd, is_secure, argc, argv, envp);
    ASSERT_NOT_REACHED();
}
}
