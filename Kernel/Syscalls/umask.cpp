/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$umask(mode_t mask)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto old_mask = protected_data.umask;
        protected_data.umask = mask & 0777;
        return old_mask;
    });
}

}
