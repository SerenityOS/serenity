/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

bool InodeMetadata::may_read(Credentials const& credentials, UseEffectiveIDs use_effective_ids) const
{
    bool eids = use_effective_ids == UseEffectiveIDs::Yes;
    return may_read(eids ? credentials.euid() : credentials.uid(), eids ? credentials.egid() : credentials.gid(), credentials.extra_gids());
}

bool InodeMetadata::may_write(Credentials const& credentials, UseEffectiveIDs use_effective_ids) const
{
    bool eids = use_effective_ids == UseEffectiveIDs::Yes;
    return may_write(eids ? credentials.euid() : credentials.uid(), eids ? credentials.egid() : credentials.gid(), credentials.extra_gids());
}

bool InodeMetadata::may_execute(Credentials const& credentials, UseEffectiveIDs use_effective_ids) const
{
    bool eids = use_effective_ids == UseEffectiveIDs::Yes;
    return may_execute(eids ? credentials.euid() : credentials.uid(), eids ? credentials.egid() : credentials.gid(), credentials.extra_gids());
}

}
