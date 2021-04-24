/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/VirtIO/VirtIORNG.h>

namespace Kernel {

VirtIORNG::VirtIORNG(PCI::Address address)
    : CharacterDevice(10, 183)
    , VirtIODevice(address, "VirtIORNG")
{
    bool success = negotiate_features([&](auto) {
        return 0;
    });
    if (success) {
        success = setup_queues(1);
    }
    if (success) {
        finish_init();
        m_entropy_buffer = MM.allocate_contiguous_kernel_region(PAGE_SIZE, "VirtIORNG", Region::Access::Read | Region::Access::Write);
        if (m_entropy_buffer) {
            memset(m_entropy_buffer->vaddr().as_ptr(), 0, m_entropy_buffer->size());
            request_entropy_from_host();
        }
    }
}

VirtIORNG::~VirtIORNG()
{
}

bool VirtIORNG::handle_device_config_change()
{
    VERIFY_NOT_REACHED(); // Device has no config
}

void VirtIORNG::handle_queue_update(u16 queue_index)
{
    VERIFY(queue_index == REQUESTQ);
    size_t available_entropy = 0, used;
    auto& queue = get_queue(REQUESTQ);
    {
        ScopedSpinLock lock(queue.lock());
        auto chain = queue.pop_used_buffer_chain(used);
        if (chain.is_empty())
            return;
        VERIFY(chain.length() == 1);
        chain.for_each([&available_entropy](PhysicalAddress, size_t length) {
            available_entropy = length;
        });
        chain.release_buffer_slots_to_queue();
    }
    dbgln_if(VIRTIO_DEBUG, "VirtIORNG: received {} bytes of entropy!", available_entropy);
    for (auto i = 0u; i < available_entropy; i++) {
        m_entropy_source.add_random_event(m_entropy_buffer->vaddr().as_ptr()[i]);
    }
    // TODO: When should we get some more entropy?
}

void VirtIORNG::request_entropy_from_host()
{
    auto& queue = get_queue(REQUESTQ);
    ScopedSpinLock lock(queue.lock());
    VirtIOQueueChain chain(queue);
    chain.add_buffer_to_chain(m_entropy_buffer->physical_page(0)->paddr(), PAGE_SIZE, BufferType::DeviceWritable);
    supply_chain_and_notify(REQUESTQ, chain);
}

}
