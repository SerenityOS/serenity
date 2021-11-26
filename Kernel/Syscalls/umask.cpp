/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$umask(mode_t mask)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    auto old_mask = m_protected_values.umask;
    ProtectedDataMutationScope scope { *this };
    m_protected_values.umask = mask & 0777;
    return old_mask;
}

}
