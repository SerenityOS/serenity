/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/Process.h>

namespace Kernel {

bool InodeMetadata::may_read(Credentials const& credentials) const
{
    return may_read(credentials.euid(), credentials.egid(), credentials.extra_gids());
}

bool InodeMetadata::may_write(Credentials const& credentials) const
{
    return may_write(credentials.euid(), credentials.egid(), credentials.extra_gids());
}

bool InodeMetadata::may_execute(Credentials const& credentials) const
{
    return may_execute(credentials.euid(), credentials.egid(), credentials.extra_gids());
}

}
