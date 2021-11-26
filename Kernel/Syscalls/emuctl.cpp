/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$emuctl()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    return ENOSYS;
}

}
