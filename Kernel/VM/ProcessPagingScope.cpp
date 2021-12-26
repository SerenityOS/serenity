/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Panic.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/ProcessPagingScope.h>

namespace Kernel {

ProcessPagingScope::ProcessPagingScope(Process& process)
{
    VERIFY(Thread::current() != nullptr);
    m_previous_cr3 = read_cr3();
    MM.enter_process_paging_scope(process);
}

ProcessPagingScope::~ProcessPagingScope()
{
    InterruptDisabler disabler;
    Thread::current()->regs().cr3 = m_previous_cr3;
    write_cr3(m_previous_cr3);
}

}
