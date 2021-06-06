/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <Kernel/Devices/KCOVInstance.h>

namespace Kernel {

KCOVInstance::KCOVInstance(ProcessID pid)
{
    m_pid = pid;
    state = UNUSED;
}

int KCOVInstance::buffer_allocate(size_t buffer_size_in_entries)
{
    // first entry contains index of last PC
    this->m_buffer_size_in_entries = buffer_size_in_entries - 1;
    this->m_buffer_size_in_bytes = page_round_up(buffer_size_in_entries * KCOV_ENTRY_SIZE);

    // one single vmobject is representing the buffer
    // - we allocate one kernel region using that vmobject
    // - when an mmap call comes in, we allocate another userspace region,
    //   backed by the same vmobject
    this->vmobject = AnonymousVMObject::try_create_with_size(
        this->m_buffer_size_in_bytes, AllocationStrategy::AllocateNow);
    if (!this->vmobject)
        return ENOMEM;

    this->m_kernel_region = MM.allocate_kernel_region_with_vmobject(
        *this->vmobject, this->m_buffer_size_in_bytes, String::formatted("kcov_{}", this->m_pid),
        Region::Access::Read | Region::Access::Write);
    if (!this->m_kernel_region)
        return ENOMEM;

    this->m_buffer = (u64*)this->m_kernel_region->vaddr().as_ptr();
    if (!this->has_buffer())
        return ENOMEM;

    return 0;
}

void KCOVInstance::buffer_add_pc(u64 pc)
{
    auto idx = (u64)this->m_buffer[0];
    if (idx >= this->m_buffer_size_in_entries) {
        // the buffer is already full
        return;
    }

    this->m_buffer[idx + 1] = pc;
    this->m_buffer[0] = idx + 1;
}

}
