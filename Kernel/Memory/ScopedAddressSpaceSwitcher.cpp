/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/InterruptDisabler.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>

namespace Kernel {

ScopedAddressSpaceSwitcher::ScopedAddressSpaceSwitcher(Process& process)
{
    VERIFY(Thread::current() != nullptr);
#if ARCH(I386) || ARCH(X86_64)
    m_previous_cr3 = read_cr3();
#elif ARCH(AARC64)
    TODO_AARCH64();
#endif
    Memory::MemoryManager::enter_process_address_space(process);
}

ScopedAddressSpaceSwitcher::~ScopedAddressSpaceSwitcher()
{
    InterruptDisabler disabler;
#if ARCH(I386) || ARCH(X86_64)
    Thread::current()->regs().cr3 = m_previous_cr3;
    write_cr3(m_previous_cr3);
#elif ARCH(AARC64)
    TODO_AARCH64();
#endif
}

}
