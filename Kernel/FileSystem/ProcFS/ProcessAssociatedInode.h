/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/ProcFS/Inode.h>
#include <Kernel/Forward.h>

namespace Kernel {

class ProcFSProcessAssociatedInode : public ProcFSInode {
    friend class ProcFS;

protected:
    ProcFSProcessAssociatedInode(ProcFS const&, ProcessID, InodeIndex);
    ProcessID associated_pid() const { return m_pid; }

    // ^Inode
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& buffer, OpenFileDescription*) override final;

private:
    const ProcessID m_pid;
};

}
