/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/File.h>

namespace Kernel {

class Inode;

class InodeFile final : public File {
public:
    static ErrorOr<NonnullRefPtr<InodeFile>> create(NonnullRefPtr<Inode>&& inode)
    {
        auto file = adopt_ref_if_nonnull(new (nothrow) InodeFile(move(inode)));
        if (!file)
            return ENOMEM;
        return file.release_nonnull();
    }

    virtual ~InodeFile() override;

    const Inode& inode() const { return *m_inode; }
    Inode& inode() { return *m_inode; }

    virtual bool can_read(const OpenFileDescription&, size_t) const override { return true; }
    virtual bool can_write(const OpenFileDescription&, size_t) const override { return true; }

    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual ErrorOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;
    virtual ErrorOr<void> stat(::stat& buffer) const override { return inode().metadata().stat(buffer); }

    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(const OpenFileDescription&) const override;

    virtual ErrorOr<void> truncate(u64) override;
    virtual ErrorOr<void> sync() override;
    virtual ErrorOr<void> chown(OpenFileDescription&, UserID, GroupID) override;
    virtual ErrorOr<void> chmod(OpenFileDescription&, mode_t) override;

    virtual StringView class_name() const override { return "InodeFile"sv; }

    virtual bool is_seekable() const override { return true; }
    virtual bool is_inode() const override { return true; }

private:
    explicit InodeFile(NonnullRefPtr<Inode>&&);
    NonnullRefPtr<Inode> m_inode;
};

}
