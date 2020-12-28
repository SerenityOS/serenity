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

#include <Kernel/FileSystem/FileBackedFileSystem.h>

namespace Kernel {

class BlockBasedFS : public FileBackedFS {
public:
    virtual ~BlockBasedFS() override;

    size_t logical_block_size() const { return m_logical_block_size; };

    virtual void flush_writes() override;
    void flush_writes_impl();

protected:
    explicit BlockBasedFS(FileDescription&);

    int read_block(unsigned index, UserOrKernelBuffer* buffer, size_t count, size_t offset = 0, bool allow_cache = true) const;
    int read_blocks(unsigned index, unsigned count, UserOrKernelBuffer& buffer, bool allow_cache = true) const;

    bool raw_read(unsigned index, UserOrKernelBuffer& buffer);
    bool raw_write(unsigned index, const UserOrKernelBuffer& buffer);

    bool raw_read_blocks(unsigned index, size_t count, UserOrKernelBuffer& buffer);
    bool raw_write_blocks(unsigned index, size_t count, const UserOrKernelBuffer& buffer);

    int write_block(unsigned index, const UserOrKernelBuffer& buffer, size_t count, size_t offset = 0, bool allow_cache = true);
    int write_blocks(unsigned index, unsigned count, const UserOrKernelBuffer&, bool allow_cache = true);

    size_t m_logical_block_size { 512 };

private:
    DiskCache& cache() const;
    void flush_specific_block_if_needed(unsigned index);

    mutable OwnPtr<DiskCache> m_cache;
};

}
