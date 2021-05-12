/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class RingBuffer {
public:
    RingBuffer(String region_name, size_t capacity);

    bool has_space() const { return m_num_used_bytes < m_capacity_in_bytes; }
    bool copy_data_in(const UserOrKernelBuffer& buffer, size_t offset, size_t length, PhysicalAddress& start_of_copied_data, size_t& bytes_copied);
    void reclaim_space(PhysicalAddress chunk_start, size_t chunk_size);
    PhysicalAddress start_of_used() const;

    SpinLock<u8>& lock() { return m_lock; }

private:
    OwnPtr<Region> m_region;
    SpinLock<u8> m_lock;
    size_t m_start_of_used {};
    size_t m_num_used_bytes {};
    size_t m_capacity_in_bytes {};
};

}
