/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/File.h>

namespace Kernel {

class Inode;

class InodeFile final : public File {
public:
    static ErrorOr<NonnullRefPtr<InodeFile>> create(NonnullRefPtr<Inode> inode)
    {
        auto file = adopt_ref_if_nonnull(new (nothrow) InodeFile(move(inode)));
        if (!file)
            return ENOMEM;
        return file.release_nonnull();
    }

    virtual ~InodeFile() override;

    Inode const& inode() const { return *m_inode; }
    Inode& inode() { return *m_inode; }

    virtual bool can_read(OpenFileDescription const&, u64) const override { return true; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }

    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual ErrorOr<VMObjectAndMemoryType> vmobject_and_memory_type_for_mmap(Process&, Memory::VirtualRange const&, u64& offset, bool shared) override;
    virtual ErrorOr<struct stat> stat() const override { return inode().metadata().stat(); }

    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const&) const override;

    virtual ErrorOr<void> truncate(u64) override;
    virtual ErrorOr<void> sync() override;
    virtual ErrorOr<void> chown(Credentials const&, OpenFileDescription&, UserID, GroupID) override;
    virtual ErrorOr<void> chmod(Credentials const&, OpenFileDescription&, mode_t) override;

    virtual StringView class_name() const override { return "InodeFile"sv; }

    virtual bool is_seekable() const override { return true; }
    virtual bool is_inode() const override { return true; }

private:
    virtual bool is_regular_file() const override;

    explicit InodeFile(NonnullRefPtr<Inode>);
    NonnullRefPtr<Inode> const m_inode;
};

}
