/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/ProcFS/FileSystem.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcFSInode : public Inode {
    friend class ProcFS;

public:
    virtual ~ProcFSInode() override;

protected:
    ProcFSInode(ProcFS const&, InodeIndex);

    ProcFS& procfs() { return static_cast<ProcFS&>(Inode::fs()); }
    ProcFS const& procfs() const { return static_cast<ProcFS const&>(Inode::fs()); }

    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override = 0;
    virtual void did_seek(OpenFileDescription&, off_t) override = 0;
    virtual ErrorOr<void> flush_metadata() override final;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override final;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override final;
    virtual ErrorOr<void> remove_child(StringView name) override final;
    virtual ErrorOr<void> replace_child(StringView name, Inode& child) override final;
    virtual ErrorOr<void> chmod(mode_t) override final;
    virtual ErrorOr<void> chown(UserID, GroupID) override final;
};

}
