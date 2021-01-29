/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
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

#include "MemoryDevice.h"
#include <AK/Memory.h>
#include <AK/StdLibExtras.h>
#include <Kernel/Arch/PC/BIOS.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/TypedMapping.h>

namespace Kernel {

MemoryDevice::MemoryDevice()
    : CharacterDevice(1, 1)
{
}

MemoryDevice::~MemoryDevice()
{
}

KResultOr<size_t> MemoryDevice::read(FileDescription&, size_t, UserOrKernelBuffer&, size_t)
{
    TODO();
}

void MemoryDevice::did_seek(FileDescription&, off_t)
{
    TODO();
}

KResultOr<Region*> MemoryDevice::mmap(Process& process, FileDescription&, const Range& range, size_t offset, int prot, bool shared)
{
    auto viewed_address = PhysicalAddress(offset);

    dbgln("MemoryDevice: Trying to mmap physical memory at {} for range of {} bytes", viewed_address, range.size());
    if (!MM.is_allowed_to_mmap_to_userspace(viewed_address, range)) {
        dbgln("MemoryDevice: Trying to mmap physical memory at {} for range of {} bytes failed due to violation of access", viewed_address, range.size());
        return EINVAL;
    }

    auto vmobject = AnonymousVMObject::create_for_physical_range(viewed_address, range.size());
    if (!vmobject)
        return ENOMEM;
    dbgln("MemoryDevice: Mapped physical memory at {} for range of {} bytes", viewed_address, range.size());
    return process.allocate_region_with_vmobject(
        range,
        vmobject.release_nonnull(),
        0,
        "Mapped Physical Memory",
        prot,
        shared);
}

}
