/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/ProcessAssociatedInode.h>

namespace Kernel {

ProcFSProcessAssociatedInode::ProcFSProcessAssociatedInode(ProcFS const& fs, ProcessID associated_pid, InodeIndex determined_index)
    : ProcFSInode(fs, determined_index)
    , m_pid(associated_pid)
{
}

ErrorOr<size_t> ProcFSProcessAssociatedInode::write_bytes_locked(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    return ENOTSUP;
}

}
