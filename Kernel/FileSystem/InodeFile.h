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

#include <Kernel/FileSystem/File.h>

class Inode;

class InodeFile final : public File {
public:
    static NonnullRefPtr<InodeFile> create(NonnullRefPtr<Inode>&& inode)
    {
        return adopt(*new InodeFile(move(inode)));
    }

    virtual ~InodeFile() override;

    const Inode& inode() const { return *m_inode; }
    Inode& inode() { return *m_inode; }

    virtual bool can_read(const FileDescription&) const override { return true; }
    virtual bool can_write(const FileDescription&) const override { return true; }

    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, VirtualAddress preferred_vaddr, size_t offset, size_t size, int prot) override;

    virtual String absolute_path(const FileDescription&) const override;

    virtual KResult truncate(u64) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult chmod(mode_t) override;

    virtual const char* class_name() const override { return "InodeFile"; }

    virtual bool is_seekable() const override { return true; }
    virtual bool is_inode() const override { return true; }

private:
    explicit InodeFile(NonnullRefPtr<Inode>&&);
    NonnullRefPtr<Inode> m_inode;
};
