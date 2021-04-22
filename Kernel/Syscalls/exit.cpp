/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/KSyms.h>
#include <Kernel/Process.h>

namespace Kernel {

void Process::sys$exit(int status)
{
    {
        ProtectedDataMutationScope scope { *this };
        m_termination_status = status;
        m_termination_signal = 0;
    }
    die();
    Thread::current()->die_if_needed();
    VERIFY_NOT_REACHED();
}

}
