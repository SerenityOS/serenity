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

#include "SharedBufferRegion.h"
#include "Emulator.h"
#include <Kernel/API/Syscall.h>
#include <serenity.h>
#include <string.h>
#include <sys/mman.h>

namespace UserspaceEmulator {

NonnullOwnPtr<SharedBufferRegion> SharedBufferRegion::create_with_shbuf_id(u32 base, u32 size, int shbuf_id, u8* host_data)
{
    return adopt_own(*new SharedBufferRegion(base, size, shbuf_id, host_data));
}

SharedBufferRegion::SharedBufferRegion(u32 base, u32 size, int shbuf_id, u8* host_data)
    : Region(base, size)
    , m_data(host_data)
    , m_shbuf_id(shbuf_id)
{
    m_shadow_data = (u8*)malloc(size);
    memset(m_shadow_data, 1, size);
}

SharedBufferRegion::~SharedBufferRegion()
{
    free(m_shadow_data);
}

ValueWithShadow<u8> SharedBufferRegion::read8(FlatPtr offset)
{
    ASSERT(offset < size());
    return { *reinterpret_cast<const u8*>(m_data + offset), *reinterpret_cast<const u8*>(m_shadow_data + offset) };
}

ValueWithShadow<u16> SharedBufferRegion::read16(u32 offset)
{
    ASSERT(offset + 1 < size());
    return { *reinterpret_cast<const u16*>(m_data + offset), *reinterpret_cast<const u16*>(m_shadow_data + offset) };
}

ValueWithShadow<u32> SharedBufferRegion::read32(u32 offset)
{
    ASSERT(offset + 3 < size());
    return { *reinterpret_cast<const u32*>(m_data + offset), *reinterpret_cast<const u32*>(m_shadow_data + offset) };
}

ValueWithShadow<u64> SharedBufferRegion::read64(u32 offset)
{
    ASSERT(offset + 7 < size());
    return { *reinterpret_cast<const u64*>(m_data + offset), *reinterpret_cast<const u64*>(m_shadow_data + offset) };
}

void SharedBufferRegion::write8(u32 offset, ValueWithShadow<u8> value)
{
    ASSERT(offset < size());
    *reinterpret_cast<u8*>(m_data + offset) = value.value();
    *reinterpret_cast<u8*>(m_shadow_data + offset) = value.shadow();
}

void SharedBufferRegion::write16(u32 offset, ValueWithShadow<u16> value)
{
    ASSERT(offset + 1 < size());
    *reinterpret_cast<u16*>(m_data + offset) = value.value();
    *reinterpret_cast<u16*>(m_shadow_data + offset) = value.shadow();
}

void SharedBufferRegion::write32(u32 offset, ValueWithShadow<u32> value)
{
    ASSERT(offset + 3 < size());
    *reinterpret_cast<u32*>(m_data + offset) = value.value();
    *reinterpret_cast<u32*>(m_shadow_data + offset) = value.shadow();
}

void SharedBufferRegion::write64(u32 offset, ValueWithShadow<u64> value)
{
    ASSERT(offset + 7 < size());
    *reinterpret_cast<u64*>(m_data + offset) = value.value();
    *reinterpret_cast<u64*>(m_shadow_data + offset) = value.shadow();
}

int SharedBufferRegion::allow_all()
{
    return syscall(SC_shbuf_allow_all, m_shbuf_id);
}

int SharedBufferRegion::allow_pid(pid_t pid)
{
    return syscall(SC_shbuf_allow_pid, m_shbuf_id, pid);
}

int SharedBufferRegion::seal()
{
    return syscall(SC_shbuf_seal, m_shbuf_id);
}

int SharedBufferRegion::release()
{
    return syscall(SC_shbuf_release, m_shbuf_id);
}

int SharedBufferRegion::set_volatile(bool is_volatile)
{
    return syscall(SC_shbuf_set_volatile, m_shbuf_id, is_volatile);
}

}
