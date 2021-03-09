/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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

#include "SoftMMU.h"
#include <sys/mman.h>

namespace UserspaceEmulator {

class MallocRegionMetadata;
class MallocTracer;

class MmapRegion final : public Region {
public:
    static NonnullOwnPtr<MmapRegion> create_anonymous(u32 base, u32 size, u32 prot);
    static NonnullOwnPtr<MmapRegion> create_file_backed(u32 base, u32 size, u32 prot, int flags, int fd, off_t offset, String name = {});
    virtual ~MmapRegion() override;

    virtual ValueWithShadow<u8> read8(u32 offset) override;
    virtual ValueWithShadow<u16> read16(u32 offset) override;
    virtual ValueWithShadow<u32> read32(u32 offset) override;
    virtual ValueWithShadow<u64> read64(u32 offset) override;

    virtual void write8(u32 offset, ValueWithShadow<u8>) override;
    virtual void write16(u32 offset, ValueWithShadow<u16>) override;
    virtual void write32(u32 offset, ValueWithShadow<u32>) override;
    virtual void write64(u32 offset, ValueWithShadow<u64>) override;

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
    void set_name(String name) { m_name = move(name); }

private:
    MmapRegion(u32 base, u32 size, int prot, u8* data, u8* shadow_data);

    u8* m_data { nullptr };
    u8* m_shadow_data { nullptr };
    bool m_file_backed { false };
    bool m_malloc { false };

    OwnPtr<MallocRegionMetadata> m_malloc_metadata;
    String m_name;
};

}
