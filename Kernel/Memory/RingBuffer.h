/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel::Memory {

class RingBuffer {
public:
    RingBuffer(String region_name, size_t capacity);

    bool has_space() const { return m_num_used_bytes < m_capacity_in_bytes; }
    bool copy_data_in(const UserOrKernelBuffer& buffer, size_t offset, size_t length, PhysicalAddress& start_of_copied_data, size_t& bytes_copied);
    ErrorOr<size_t> copy_data_out(size_t size, UserOrKernelBuffer& buffer) const;
    ErrorOr<PhysicalAddress> reserve_space(size_t size);
    void reclaim_space(PhysicalAddress chunk_start, size_t chunk_size);
    PhysicalAddress start_of_used() const;

    Spinlock& lock() { return m_lock; }
    size_t used_bytes() const { return m_num_used_bytes; }
    PhysicalAddress start_of_region() const { return m_region->physical_page(0)->paddr(); }
    VirtualAddress vaddr() const { return m_region->vaddr(); }
    size_t bytes_till_end() const { return (m_capacity_in_bytes - ((m_start_of_used + m_num_used_bytes) % m_capacity_in_bytes)) % m_capacity_in_bytes; };

private:
    OwnPtr<Memory::Region> m_region;
    Spinlock m_lock;
    size_t m_start_of_used {};
    size_t m_num_used_bytes {};
    size_t m_capacity_in_bytes {};
};

}
