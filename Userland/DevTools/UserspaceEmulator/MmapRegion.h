/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SoftMMU.h"
#include <sys/mman.h>

namespace UserspaceEmulator {

class MallocRegionMetadata;
class MallocTracer;

class MmapRegion final : public Region {
public:
    static NonnullOwnPtr<MmapRegion> create_anonymous(u32 base, u32 size, u32 prot, String name);
    static NonnullOwnPtr<MmapRegion> create_file_backed(u32 base, u32 size, u32 prot, int flags, int fd, off_t offset, String name);
    virtual ~MmapRegion() override;

    virtual ValueWithShadow<u8> read8(u32 offset) override;
    virtual ValueWithShadow<u16> read16(u32 offset) override;
    virtual ValueWithShadow<u32> read32(u32 offset) override;
    virtual ValueWithShadow<u64> read64(u32 offset) override;
    virtual ValueWithShadow<u128> read128(u32 offset) override;
    virtual ValueWithShadow<u256> read256(u32 offset) override;

    virtual void write8(u32 offset, ValueWithShadow<u8>) override;
    virtual void write16(u32 offset, ValueWithShadow<u16>) override;
    virtual void write32(u32 offset, ValueWithShadow<u32>) override;
    virtual void write64(u32 offset, ValueWithShadow<u64>) override;
    virtual void write128(u32 offset, ValueWithShadow<u128>) override;
    virtual void write256(u32 offset, ValueWithShadow<u256>) override;

    virtual u8* data() override { return m_data; }
    virtual u8* shadow_data() override { return m_shadow_data; }

    bool is_malloc_block() const { return m_malloc; }
    void set_malloc(bool b) { m_malloc = b; }

    NonnullOwnPtr<MmapRegion> split_at(VirtualAddress);

    int prot() const
    {
        return (is_readable() ? PROT_READ : 0) | (is_writable() ? PROT_WRITE : 0) | (is_executable() ? PROT_EXEC : 0);
    }
    void set_prot(int prot);

    MallocRegionMetadata* malloc_metadata() { return m_malloc_metadata; }
    void set_malloc_metadata(Badge<MallocTracer>, NonnullOwnPtr<MallocRegionMetadata> metadata) { m_malloc_metadata = move(metadata); }

    const String& name() const { return m_name; }
    String lib_name() const
    {
        if (m_name.contains("Loader.so"sv))
            return "Loader.so";
        auto const maybe_separator = m_name.find(':');
        if (!maybe_separator.has_value())
            return {};
        return m_name.substring(0, *maybe_separator);
    }
    void set_name(String name);

private:
    MmapRegion(u32 base, u32 size, int prot, u8* data, u8* shadow_data);

    u8* m_data { nullptr };
    u8* m_shadow_data { nullptr };
    bool m_file_backed { false };
    bool m_malloc { false };

    OwnPtr<MallocRegionMetadata> m_malloc_metadata;
    String m_name;
};

template<>
inline bool Region::fast_is<MmapRegion>() const { return m_mmap; }

}
