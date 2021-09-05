/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/VirtIO/RNG.h>
#include <Kernel/Sections.h>

namespace Kernel::VirtIO {

UNMAP_AFTER_INIT RefPtr<RNG> RNG::try_create(PCI::Address address)
{
    auto entropy_buffer = MM.allocate_contiguous_kernel_region(PAGE_SIZE, "VirtIO::RNG", Memory::Region::Access::ReadWrite);
    if (entropy_buffer.is_error())
        return {};
    auto rng = adopt_ref_if_nonnull(new RNG(address, entropy_buffer.release_value()));
    if (!rng)
        return {};
    if (!rng->initialize())
        return {};
    return rng;
}

UNMAP_AFTER_INIT bool RNG::initialize()
{
    if (auto result = Device::initialize(); !result)
        return false;
    bool success = negotiate_features([&](auto) {
        return 0;
    });
    if (success) {
        success = setup_queues(1);
    }
    if (success) {
        finish_init();
        memset(m_entropy_buffer->vaddr().as_ptr(), 0, m_entropy_buffer->size());
        request_entropy_from_host();
    }
    return true;
}

UNMAP_AFTER_INIT RNG::RNG(PCI::Address address, NonnullOwnPtr<Memory::Region>&& entropy_buffer_region)
    : VirtIO::Device(address)
    , m_entropy_buffer(move(entropy_buffer_region))
{
}

bool RNG::handle_device_config_change()
{
    return false; // Device has no config
}

void RNG::handle_queue_update(u16 queue_index)
{
    VERIFY(queue_index == REQUESTQ);
    size_t available_entropy = 0, used;
    auto& queue = get_queue(REQUESTQ);
    {
        SpinlockLocker lock(queue.lock());
        auto chain = queue.pop_used_buffer_chain(used);
        if (chain.is_empty())
            return;
        VERIFY(chain.length() == 1);
        chain.for_each([&available_entropy](PhysicalAddress, size_t length) {
            available_entropy = length;
        });
        chain.release_buffer_slots_to_queue();
    }
    dbgln_if(VIRTIO_DEBUG, "VirtIO::RNG: received {} bytes of entropy!", available_entropy);
    for (auto i = 0u; i < available_entropy; i++) {
        m_entropy_source.add_random_event(m_entropy_buffer->vaddr().as_ptr()[i]);
    }
    // TODO: When should we get some more entropy?
}

void RNG::request_entropy_from_host()
{
    auto& queue = get_queue(REQUESTQ);
    SpinlockLocker lock(queue.lock());
    QueueChain chain(queue);
    chain.add_buffer_to_chain(m_entropy_buffer->physical_page(0)->paddr(), PAGE_SIZE, BufferType::DeviceWritable);
    supply_chain_and_notify(REQUESTQ, chain);
}

}
