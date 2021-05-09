/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

KResultOr<mode_t> Process::sys$umask(mode_t mask)
{
    REQUIRE_PROMISE(stdio);
    auto old_mask = m_umask;
    ProtectedDataMutationScope scope { *this };
    m_umask = mask & 0777;
    return old_mask;
}

}
