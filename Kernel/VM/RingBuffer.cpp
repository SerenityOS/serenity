/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/UserOrKernelBuffer.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/RingBuffer.h>

namespace Kernel {

RingBuffer::RingBuffer(String region_name, size_t capacity)
    : m_region(MM.allocate_contiguous_kernel_region(page_round_up(capacity), move(region_name), Region::Access::Read | Region::Access::Write))
    , m_capacity_in_bytes(capacity)
{
}

bool RingBuffer::copy_data_in(const UserOrKernelBuffer& buffer, size_t offset, size_t length, PhysicalAddress& start_of_copied_data, size_t& bytes_copied)
{
    size_t start_of_free_area = (m_start_of_used + m_num_used_bytes) % m_capacity_in_bytes;
    bytes_copied = min(m_capacity_in_bytes - m_num_used_bytes, min(m_capacity_in_bytes - start_of_free_area, length));
    if (bytes_copied == 0)
        return false;
    if (buffer.read(m_region->vaddr().offset(start_of_free_area).as_ptr(), offset, bytes_copied)) {
        m_num_used_bytes += bytes_copied;
        start_of_copied_data = m_region->physical_page(start_of_free_area / PAGE_SIZE)->paddr().offset(start_of_free_area % PAGE_SIZE);
        return true;
    }
    return false;
}

void RingBuffer::reclaim_space(PhysicalAddress chunk_start, size_t chunk_size)
{
    VERIFY(start_of_used() == chunk_start);
    VERIFY(m_num_used_bytes >= chunk_size);
    m_num_used_bytes -= chunk_size;
    m_start_of_used += chunk_size;
}

PhysicalAddress RingBuffer::start_of_used() const
{
    size_t start = m_start_of_used % m_capacity_in_bytes;
    return m_region->physical_page(start / PAGE_SIZE)->paddr().offset(start % PAGE_SIZE);
}

}
