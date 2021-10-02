/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>

namespace Kernel {

ScopedAddressSpaceSwitcher::ScopedAddressSpaceSwitcher(Process& process)
{
    VERIFY(Thread::current() != nullptr);
    m_previous_cr3 = read_cr3();
    Memory::MemoryManager::enter_process_address_space(process);
}

ScopedAddressSpaceSwitcher::~ScopedAddressSpaceSwitcher()
{
    InterruptDisabler disabler;
    Thread::current()->regs().cr3 = m_previous_cr3;
    write_cr3(m_previous_cr3);
}

}
