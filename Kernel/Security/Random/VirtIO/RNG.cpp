/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random/VirtIO/RNG.h>

namespace Kernel::VirtIO {

UNMAP_AFTER_INIT NonnullLockRefPtr<RNG> RNG::must_create_for_pci_instance(PCI::DeviceIdentifier const& device_identifier)
{
    auto pci_transport_link = MUST(PCIeTransportLink::create(device_identifier));
    return adopt_lock_ref_if_nonnull(new RNG(move(pci_transport_link))).release_nonnull();
}

UNMAP_AFTER_INIT ErrorOr<void> RNG::initialize_virtio_resources()
{
    TRY(Device::initialize_virtio_resources());
    TRY(negotiate_features([&](auto) {
        return 0;
    }));
    TRY(setup_queues(1));
    finish_init();
    m_entropy_buffer = TRY(MM.allocate_contiguous_kernel_region(PAGE_SIZE, "VirtIO::RNG"sv, Memory::Region::Access::ReadWrite));
    memset(m_entropy_buffer->vaddr().as_ptr(), 0, m_entropy_buffer->size());
    request_entropy_from_host();
    return {};
}

UNMAP_AFTER_INIT RNG::RNG(NonnullOwnPtr<TransportEntity> transport_entity)
    : VirtIO::Device(move(transport_entity))
{
}

ErrorOr<void> RNG::handle_device_config_change()
{
    return Error::from_errno(EIO); // Device has no config
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
