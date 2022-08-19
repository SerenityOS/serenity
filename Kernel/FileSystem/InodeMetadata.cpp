/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/Process.h>

namespace Kernel {

bool InodeMetadata::may_read(Process const& process) const
{
    return may_read(process.euid(), process.egid(), process.extra_gids());
}

bool InodeMetadata::may_write(Process const& process) const
{
    return may_write(process.euid(), process.egid(), process.extra_gids());
}

bool InodeMetadata::may_execute(Process const& process) const
{
    return may_execute(process.euid(), process.egid(), process.extra_gids());
}

}
