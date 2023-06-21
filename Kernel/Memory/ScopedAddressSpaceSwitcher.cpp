/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>

namespace Kernel {

ScopedAddressSpaceSwitcher::ScopedAddressSpaceSwitcher(Process& process)
{
    VERIFY(Thread::current() != nullptr);
    m_previous_page_directory = Memory::PageDirectory::find_current();
    Memory::MemoryManager::enter_process_address_space(process);
}

ScopedAddressSpaceSwitcher::~ScopedAddressSpaceSwitcher()
{
    InterruptDisabler disabler;
    Memory::activate_page_directory(*m_previous_page_directory, Thread::current());
}

}
