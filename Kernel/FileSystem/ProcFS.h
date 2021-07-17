/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Forward.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Mutex.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

class ProcFS final : public FileSystem {
    friend class ProcFSInode;
    friend class ProcFSDirectoryInode;

public:
    virtual ~ProcFS() override;
    static RefPtr<ProcFS> create();

    virtual bool initialize() override;
    virtual StringView class_name() const override { return "ProcFS"sv; }

    virtual NonnullRefPtr<Inode> root_inode() const override;

private:
    ProcFS();

    NonnullRefPtr<ProcFSDirectoryInode> m_root_inode;
};

class ProcFSInode : public Inode {
    friend class ProcFS;

public:
    static NonnullRefPtr<ProcFSInode> create(const ProcFS&, const ProcFSExposedComponent&);
    virtual ~ProcFSInode() override;
    StringView name() const;

protected:
    ProcFSInode(const ProcFS&, const ProcFSExposedComponent&);

    // ^Inode
    virtual KResult attach(FileDescription& description) override;
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual InodeMetadata metadata() const override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& buffer, FileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(const String& name, mode_t, dev_t, uid_t, gid_t) override;
    virtual KResult add_child(Inode&, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual void did_seek(FileDescription&, off_t) override;
    virtual KResultOr<size_t> directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult truncate(u64) override;

    NonnullRefPtr<ProcFSExposedComponent> m_associated_component;
};

class ProcFSLinkInode : public ProcFSInode {
    friend class ProcFS;

public:
    static NonnullRefPtr<ProcFSLinkInode> create(const ProcFS&, const ProcFSExposedComponent&);

protected:
    ProcFSLinkInode(const ProcFS&, const ProcFSExposedComponent&);
    virtual InodeMetadata metadata() const override;
};

class ProcFSDirectoryInode : public ProcFSInode {
    friend class ProcFS;

public:
    static NonnullRefPtr<ProcFSDirectoryInode> create(const ProcFS&, const ProcFSExposedComponent&);
    virtual ~ProcFSDirectoryInode() override;

protected:
    ProcFSDirectoryInode(const ProcFS&, const ProcFSExposedComponent&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual KResultOr<size_t> directory_entry_count() const override;

    ProcFS& m_parent_fs;
};

}
