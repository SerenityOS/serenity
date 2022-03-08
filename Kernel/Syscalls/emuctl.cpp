/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$emuctl()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    return ENOSYS;
}

}
