/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MmapRegion.h"
#include "Emulator.h"
#include <string.h>
#include <sys/mman.h>

namespace UserspaceEmulator {

static void* mmap_initialized(size_t bytes, char initial_value, const char* name)
{
    auto* ptr = mmap_with_name(nullptr, bytes, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, name);
    VERIFY(ptr != MAP_FAILED);
    memset(ptr, initial_value, bytes);
    return ptr;
}

static void free_pages(void* ptr, size_t bytes)
{
    int rc = munmap(ptr, bytes);
    VERIFY(rc == 0);
}

NonnullOwnPtr<MmapRegion> MmapRegion::create_anonymous(u32 base, u32 size, u32 prot, String name)
{
    auto data = (u8*)mmap_initialized(size, 0, nullptr);
    auto shadow_data = (u8*)mmap_initialized(size, 1, "MmapRegion ShadowData");
    auto region = adopt_own(*new MmapRegion(base, size, prot, data, shadow_data));
    region->m_name = move(name);
    return region;
}

NonnullOwnPtr<MmapRegion> MmapRegion::create_file_backed(u32 base, u32 size, u32 prot, int flags, int fd, off_t offset, String name)
{
    // Since we put the memory to an arbitrary location, do not pass MAP_FIXED to the Kernel.
    auto real_flags = flags & ~MAP_FIXED;
    auto data = (u8*)mmap_with_name(nullptr, size, prot, real_flags, fd, offset, name.is_empty() ? nullptr : name.characters());
    VERIFY(data != MAP_FAILED);
    auto shadow_data = (u8*)mmap_initialized(size, 1, "MmapRegion ShadowData");
    auto region = adopt_own(*new MmapRegion(base, size, prot, data, shadow_data));
    region->m_file_backed = true;
    region->m_name = move(name);
    return region;
}

MmapRegion::MmapRegion(u32 base, u32 size, int prot, u8* data, u8* shadow_data)
    : Region(base, size, true)
    , m_data(data)
    , m_shadow_data(shadow_data)
{
    set_prot(prot);
}

MmapRegion::~MmapRegion()
{
    free_pages(m_data, size());
    free_pages(m_shadow_data, size());
}

ValueWithShadow<u8> MmapRegion::read8(FlatPtr offset)
{
    if (!is_readable()) {
        reportln("8-bit read from unreadable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_read(*this, base() + offset, 1);
    }

    VERIFY(offset < size());
    return { *reinterpret_cast<const u8*>(m_data + offset), *reinterpret_cast<const u8*>(m_shadow_data + offset) };
}

ValueWithShadow<u16> MmapRegion::read16(u32 offset)
{
    if (!is_readable()) {
        reportln("16-bit read from unreadable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_read(*this, base() + offset, 2);
    }

    VERIFY(offset + 1 < size());
    return { *reinterpret_cast<const u16*>(m_data + offset), *reinterpret_cast<const u16*>(m_shadow_data + offset) };
}

ValueWithShadow<u32> MmapRegion::read32(u32 offset)
{
    if (!is_readable()) {
        reportln("32-bit read from unreadable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_read(*this, base() + offset, 4);
    }

    VERIFY(offset + 3 < size());
    return { *reinterpret_cast<const u32*>(m_data + offset), *reinterpret_cast<const u32*>(m_shadow_data + offset) };
}

ValueWithShadow<u64> MmapRegion::read64(u32 offset)
{
    if (!is_readable()) {
        reportln("64-bit read from unreadable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_read(*this, base() + offset, 8);
    }

    VERIFY(offset + 7 < size());
    return { *reinterpret_cast<const u64*>(m_data + offset), *reinterpret_cast<const u64*>(m_shadow_data + offset) };
}

ValueWithShadow<u128> MmapRegion::read128(u32 offset)
{
    if (!is_readable()) {
        reportln("128-bit read from unreadable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_read(*this, base() + offset, 16);
    }

    VERIFY(offset + 15 < size());
    return { *reinterpret_cast<const u128*>(m_data + offset), *reinterpret_cast<const u128*>(m_shadow_data + offset) };
}

ValueWithShadow<u256> MmapRegion::read256(u32 offset)
{
    if (!is_readable()) {
        reportln("256-bit read from unreadable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_read(*this, base() + offset, 32);
    }

    VERIFY(offset + 31 < size());
    return { *reinterpret_cast<const u256*>(m_data + offset), *reinterpret_cast<const u256*>(m_shadow_data + offset) };
}

void MmapRegion::write8(u32 offset, ValueWithShadow<u8> value)
{
    if (!is_writable()) {
        reportln("8-bit write from unwritable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_write(*this, base() + offset, 1);
    }

    VERIFY(offset < size());
    *reinterpret_cast<u8*>(m_data + offset) = value.value();
    *reinterpret_cast<u8*>(m_shadow_data + offset) = value.shadow();
}

void MmapRegion::write16(u32 offset, ValueWithShadow<u16> value)
{
    if (!is_writable()) {
        reportln("16-bit write from unwritable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_write(*this, base() + offset, 2);
    }

    VERIFY(offset + 1 < size());
    *reinterpret_cast<u16*>(m_data + offset) = value.value();
    *reinterpret_cast<u16*>(m_shadow_data + offset) = value.shadow();
}

void MmapRegion::write32(u32 offset, ValueWithShadow<u32> value)
{
    if (!is_writable()) {
        reportln("32-bit write from unwritable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_write(*this, base() + offset, 4);
    }

    VERIFY(offset + 3 < size());
    VERIFY(m_data != m_shadow_data);
    *reinterpret_cast<u32*>(m_data + offset) = value.value();
    *reinterpret_cast<u32*>(m_shadow_data + offset) = value.shadow();
}

void MmapRegion::write64(u32 offset, ValueWithShadow<u64> value)
{
    if (!is_writable()) {
        reportln("64-bit write from unwritable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_write(*this, base() + offset, 8);
    }

    VERIFY(offset + 7 < size());
    VERIFY(m_data != m_shadow_data);
    *reinterpret_cast<u64*>(m_data + offset) = value.value();
    *reinterpret_cast<u64*>(m_shadow_data + offset) = value.shadow();
}

void MmapRegion::write128(u32 offset, ValueWithShadow<u128> value)
{
    if (!is_writable()) {
        reportln("128-bit write from unwritable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_write(*this, base() + offset, 16);
    }

    VERIFY(offset + 15 < size());
    VERIFY(m_data != m_shadow_data);
    *reinterpret_cast<u128*>(m_data + offset) = value.value();
    *reinterpret_cast<u128*>(m_shadow_data + offset) = value.shadow();
}

void MmapRegion::write256(u32 offset, ValueWithShadow<u256> value)
{
    if (!is_writable()) {
        reportln("256-bit write from unwritable MmapRegion @ {:p}", base() + offset);
        emulator().dump_backtrace();
        TODO();
    }

    if (is_malloc_block()) {
        if (auto* tracer = emulator().malloc_tracer())
            tracer->audit_write(*this, base() + offset, 32);
    }

    VERIFY(offset + 31 < size());
    VERIFY(m_data != m_shadow_data);
    *reinterpret_cast<u256*>(m_data + offset) = value.value();
    *reinterpret_cast<u256*>(m_shadow_data + offset) = value.shadow();
}

NonnullOwnPtr<MmapRegion> MmapRegion::split_at(VirtualAddress offset)
{
    VERIFY(!m_malloc);
    VERIFY(!m_malloc_metadata);
    Range new_range = range();
    Range other_range = new_range.split_at(offset);
    auto other_region = adopt_own(*new MmapRegion(other_range.base().get(), other_range.size(), prot(), data() + new_range.size(), shadow_data() + new_range.size()));
    other_region->m_file_backed = m_file_backed;
    other_region->m_name = m_name;
    set_range(new_range);
    return other_region;
}

void MmapRegion::set_prot(int prot)
{
    set_readable(prot & PROT_READ);
    set_writable(prot & PROT_WRITE);
    set_executable(prot & PROT_EXEC);
    if (m_file_backed) {
        if (mprotect(m_data, size(), prot & ~PROT_EXEC) < 0) {
            perror("MmapRegion::set_prot: mprotect");
            exit(1);
        }
    }
}

}
