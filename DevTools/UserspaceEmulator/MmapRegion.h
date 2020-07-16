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

#include "SoftMMU.h"
#include <sys/mman.h>

namespace UserspaceEmulator {

class MmapRegion final : public SoftMMU::Region {
public:
    static NonnullOwnPtr<MmapRegion> create_anonymous(u32 base, u32 size, u32 prot);
    static NonnullOwnPtr<MmapRegion> create_file_backed(u32 base, u32 size, u32 prot, int flags, int fd, off_t offset);
    virtual ~MmapRegion() override;

    virtual u8 read8(u32 offset) override;
    virtual u16 read16(u32 offset) override;
    virtual u32 read32(u32 offset) override;

    virtual void write8(u32 offset, u8 value) override;
    virtual void write16(u32 offset, u16 value) override;
    virtual void write32(u32 offset, u32 value) override;

    u8* data() { return m_data; }

    bool is_readable() const { return m_prot & PROT_READ; }
    bool is_writable() const { return m_prot & PROT_WRITE; }
    bool is_executable() const { return m_prot & PROT_EXEC; }

    bool is_malloc_block() const;

private:
    MmapRegion(u32 base, u32 size, int prot);
    virtual bool is_mmap() const override { return true; }

    u8* m_data { nullptr };
    int m_prot { 0 };
    bool m_file_backed { false };
};

}
