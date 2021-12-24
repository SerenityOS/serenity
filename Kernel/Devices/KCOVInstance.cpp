/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/KCOVInstance.h>

namespace Kernel {

KCOVInstance::KCOVInstance(ProcessID pid)
{
    m_pid = pid;
}

ErrorOr<void> KCOVInstance::buffer_allocate(size_t buffer_size_in_entries)
{
    if (buffer_size_in_entries < 2 || buffer_size_in_entries > KCOV_MAX_ENTRIES)
        return EINVAL;

    // first entry contains index of last PC
    m_buffer_size_in_entries = buffer_size_in_entries - 1;
    m_buffer_size_in_bytes = TRY(Memory::page_round_up(buffer_size_in_entries * KCOV_ENTRY_SIZE));

    // one single vmobject is representing the buffer
    // - we allocate one kernel region using that vmobject
    // - when an mmap call comes in, we allocate another userspace region,
    //   backed by the same vmobject
    m_vmobject = TRY(Memory::AnonymousVMObject::try_create_with_size(m_buffer_size_in_bytes, AllocationStrategy::AllocateNow));

    auto region_name = TRY(KString::formatted("kcov_{}", m_pid));
    m_kernel_region = TRY(MM.allocate_kernel_region_with_vmobject(
        *m_vmobject, m_buffer_size_in_bytes, region_name->view(),
        Memory::Region::Access::ReadWrite));

    m_buffer = (u64*)m_kernel_region->vaddr().as_ptr();
    return {};
}

void KCOVInstance::buffer_add_pc(u64 pc)
{
    auto idx = (u64)m_buffer[0];
    if (idx >= m_buffer_size_in_entries) {
        // the buffer is already full
        return;
    }

    m_buffer[idx + 1] = pc;
    m_buffer[0] = idx + 1;
}

}
