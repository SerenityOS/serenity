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

#include "MmapRegion.h"
#include "Emulator.h"
#include <string.h>
#include <sys/mman.h>

namespace UserspaceEmulator {

NonnullOwnPtr<MmapRegion> MmapRegion::create_anonymous(u32 base, u32 size, u32 prot)
{
    auto region = adopt_own(*new MmapRegion(base, size, prot));
    region->m_file_backed = false;
    region->m_data = (u8*)calloc(1, size);
    return region;
}

NonnullOwnPtr<MmapRegion> MmapRegion::create_file_backed(u32 base, u32 size, u32 prot, int flags, int fd, off_t offset)
{
    auto region = adopt_own(*new MmapRegion(base, size, prot));
    region->m_file_backed = true;
    region->m_data = (u8*)mmap(nullptr, size, prot, flags, fd, offset);
    ASSERT(region->m_data != MAP_FAILED);
    return region;
}

MmapRegion::MmapRegion(u32 base, u32 size, int prot)
    : Region(base, size)
    , m_prot(prot)
{
    m_shadow_data = (u8*)malloc(size);
    memset(m_shadow_data, 1, size);
}

MmapRegion::~MmapRegion()
{
    free(m_shadow_data);
    if (m_file_backed)
        munmap(m_data, size());
    else
        free(m_data);
}

ValueWithShadow<u8> MmapRegion::read8(FlatPtr offset)
{
    if (!is_readable()) {
        warnln("8-bit read from unreadable MmapRegion @ {:p}", base() + offset);
        Emulator::the().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = Emulator::the().malloc_tracer())
            tracer->audit_read(base() + offset, 1);
    }

    ASSERT(offset < size());
    return { *reinterpret_cast<const u8*>(m_data + offset), *reinterpret_cast<const u8*>(m_shadow_data + offset) };
}

ValueWithShadow<u16> MmapRegion::read16(u32 offset)
{
    if (!is_readable()) {
        warnln("16-bit read from unreadable MmapRegion @ {:p}", base() + offset);
        Emulator::the().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = Emulator::the().malloc_tracer())
            tracer->audit_read(base() + offset, 2);
    }

    ASSERT(offset + 1 < size());
    return { *reinterpret_cast<const u16*>(m_data + offset), *reinterpret_cast<const u16*>(m_shadow_data + offset) };
}

ValueWithShadow<u32> MmapRegion::read32(u32 offset)
{
    if (!is_readable()) {
        warnln("32-bit read from unreadable MmapRegion @ {:p}", base() + offset);
        Emulator::the().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = Emulator::the().malloc_tracer())
            tracer->audit_read(base() + offset, 4);
    }

    ASSERT(offset + 3 < size());
    return { *reinterpret_cast<const u32*>(m_data + offset), *reinterpret_cast<const u32*>(m_shadow_data + offset) };
}

ValueWithShadow<u64> MmapRegion::read64(u32 offset)
{
    if (!is_readable()) {
        warnln("64-bit read from unreadable MmapRegion @ {:p}", base() + offset);
        Emulator::the().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = Emulator::the().malloc_tracer())
            tracer->audit_read(base() + offset, 8);
    }

    ASSERT(offset + 7 < size());
    return { *reinterpret_cast<const u64*>(m_data + offset), *reinterpret_cast<const u64*>(m_shadow_data + offset) };
}

void MmapRegion::write8(u32 offset, ValueWithShadow<u8> value)
{
    if (!is_writable()) {
        warnln("8-bit write from unwritable MmapRegion @ {:p}", base() + offset);
        Emulator::the().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = Emulator::the().malloc_tracer())
            tracer->audit_write(base() + offset, 1);
    }

    ASSERT(offset < size());
    *reinterpret_cast<u8*>(m_data + offset) = value.value();
    *reinterpret_cast<u8*>(m_shadow_data + offset) = value.shadow();
}

void MmapRegion::write16(u32 offset, ValueWithShadow<u16> value)
{
    if (!is_writable()) {
        warnln("16-bit write from unwritable MmapRegion @ {:p}", base() + offset);
        Emulator::the().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = Emulator::the().malloc_tracer())
            tracer->audit_write(base() + offset, 2);
    }

    ASSERT(offset + 1 < size());
    *reinterpret_cast<u16*>(m_data + offset) = value.value();
    *reinterpret_cast<u16*>(m_shadow_data + offset) = value.shadow();
}

void MmapRegion::write32(u32 offset, ValueWithShadow<u32> value)
{
    if (!is_writable()) {
        warnln("32-bit write from unwritable MmapRegion @ {:p}", base() + offset);
        Emulator::the().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = Emulator::the().malloc_tracer())
            tracer->audit_write(base() + offset, 4);
    }

    ASSERT(offset + 3 < size());
    ASSERT(m_data != m_shadow_data);
    *reinterpret_cast<u32*>(m_data + offset) = value.value();
    *reinterpret_cast<u32*>(m_shadow_data + offset) = value.shadow();
}

void MmapRegion::write64(u32 offset, ValueWithShadow<u64> value)
{
    if (!is_writable()) {
        warnln("64-bit write from unwritable MmapRegion @ {:p}", base() + offset);
        Emulator::the().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = Emulator::the().malloc_tracer())
            tracer->audit_write(base() + offset, 8);
    }

    ASSERT(offset + 7 < size());
    ASSERT(m_data != m_shadow_data);
    *reinterpret_cast<u64*>(m_data + offset) = value.value();
    *reinterpret_cast<u64*>(m_shadow_data + offset) = value.shadow();
}

}
