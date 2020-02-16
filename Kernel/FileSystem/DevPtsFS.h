/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

class SlavePTY;
class DevPtsFSInode;

class DevPtsFS final : public FS {
public:
    virtual ~DevPtsFS() override;
    static NonnullRefPtr<DevPtsFS> create();

    virtual bool initialize() override;
    virtual const char* class_name() const override { return "DevPtsFS"; }

    virtual InodeIdentifier root_inode() const override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_inode(InodeIdentifier parent_id, const String& name, mode_t, off_t size, dev_t, uid_t, gid_t) override;
    virtual KResult create_directory(InodeIdentifier parent_inode, const String& name, mode_t, uid_t, gid_t) override;
    virtual RefPtr<Inode> get_inode(InodeIdentifier) const override;

    static void register_slave_pty(SlavePTY&);
    static void unregister_slave_pty(SlavePTY&);

private:
    DevPtsFS();

    RefPtr<DevPtsFSInode> m_root_inode;
};

class DevPtsFSInode final : public Inode {
    friend class DevPtsFS;

public:
    virtual ~DevPtsFSInode() override;

private:
    DevPtsFSInode(DevPtsFS&, unsigned index);

    // ^Inode
    virtual ssize_t read_bytes(off_t, ssize_t, u8* buffer, FileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual ssize_t write_bytes(off_t, ssize_t, const u8* buffer, FileDescription*) override;
    virtual KResult add_child(InodeIdentifier child_id, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual size_t directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;

    InodeMetadata m_metadata;
};

}
