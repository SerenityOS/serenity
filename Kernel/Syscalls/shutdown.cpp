/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/ACPI/Parser.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/IO.h>
#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$reboot()
{
    if (!is_superuser())
        return -EPERM;

    REQUIRE_NO_PROMISES;

    dbg() << "acquiring FS locks...";
    FS::lock_all();
    dbg() << "syncing mounted filesystems...";
    FS::sync();
    dbg() << "attempting reboot via ACPI";
    if (ACPI::is_enabled())
        ACPI::Parser::the()->try_acpi_reboot();
    dbg() << "attempting reboot via KB Controller...";
    IO::out8(0x64, 0xFE);

    return 0;
}

int Process::sys$halt()
{
    if (!is_superuser())
        return -EPERM;

    REQUIRE_NO_PROMISES;

    dbg() << "acquiring FS locks...";
    FS::lock_all();
    dbg() << "syncing mounted filesystems...";
    FS::sync();
    dbg() << "attempting system shutdown...";
    // QEMU Shutdown
    IO::out16(0x604, 0x2000);
    // If we're here, the shutdown failed. Try VirtualBox shutdown.
    IO::out16(0x4004, 0x3400);
    // VirtualBox shutdown failed. Try Bochs/Old QEMU shutdown.
    IO::out16(0xb004, 0x2000);
    dbg() << "shutdown attempts failed, applications will stop responding.";

    return 0;
}

}
