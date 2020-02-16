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

#pragma once

#include "FileSystem.h"
#include <AK/ByteBuffer.h>

namespace Kernel {

class DiskCache;

class DiskBackedFS : public FS {
public:
    virtual ~DiskBackedFS() override;

    virtual bool is_disk_backed() const override { return true; }

    BlockDevice& device() { return *m_device; }
    const BlockDevice& device() const { return *m_device; }

    virtual void flush_writes() override;

    void flush_writes_impl();

protected:
    explicit DiskBackedFS(BlockDevice&);

    bool read_block(unsigned index, u8* buffer, FileDescription* = nullptr) const;
    bool read_blocks(unsigned index, unsigned count, u8* buffer, FileDescription* = nullptr) const;

    bool write_block(unsigned index, const u8*, FileDescription* = nullptr);
    bool write_blocks(unsigned index, unsigned count, const u8*, FileDescription* = nullptr);

private:
    DiskCache& cache() const;
    void flush_specific_block_if_needed(unsigned index);

    NonnullRefPtr<BlockDevice> m_device;
    mutable OwnPtr<DiskCache> m_cache;
};

}
