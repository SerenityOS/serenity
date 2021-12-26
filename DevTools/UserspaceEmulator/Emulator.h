/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include "SoftCPU.h"
#include "SoftMMU.h"
#include <AK/Types.h>
#include <LibELF/Loader.h>
#include <LibX86/Instruction.h>
#include <sys/types.h>

namespace UserspaceEmulator {

class Emulator {
public:
    Emulator(const String& executable_path, NonnullRefPtr<ELF::Loader>);

    bool load_elf();
    void dump_backtrace();

    int exec();
    u32 virt_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3);

    SoftMMU& mmu() { return m_mmu; }

private:
    NonnullRefPtr<ELF::Loader> m_elf;

    SoftMMU m_mmu;
    SoftCPU m_cpu;

    void setup_stack();

    u32 virt$mmap(u32);
    u32 virt$gettid();
    u32 virt$unveil(u32);
    u32 virt$pledge(u32);
    uid_t virt$getuid();
    gid_t virt$getgid();
    u32 virt$read(int, FlatPtr, ssize_t);
    u32 virt$write(int, FlatPtr, ssize_t);
    u32 virt$mprotect(FlatPtr, size_t, int);
    u32 virt$madvise(FlatPtr, size_t, int);
    void virt$exit(int);

    bool m_shutdown { false };
    int m_exit_status { 0 };

    String m_executable_path;
};

}
