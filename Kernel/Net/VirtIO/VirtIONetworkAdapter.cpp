/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MACAddress.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Debug.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/VirtIO/VirtIONetworkAdapter.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define VIRTIO_NET_F_MTU (1 << 3)
#define VIRTIO_NET_F_MAC (1 << 5)
#define VIRTIO_NET_F_STATUS (1 << 16)

#define VIRTIO_NET_S_LINK_UP 1
#define VIRTIO_NET_S_ANNOUNCE 2

#define PACKET_SIZE_MAX 0x600

UNMAP_AFTER_INIT RefPtr<VirtIONetworkAdapter> VirtIONetworkAdapter::try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::VirtIO || pci_device_identifier.hardware_id().device_id != PCI::DeviceID::VirtIONetAdapter)
        return {};

    auto interface_name_or_error = NetworkingManagement::generate_interface_name_from_pci_address(pci_device_identifier);
    if (interface_name_or_error.is_error())
        return {};

    auto adapter = adopt_ref_if_nonnull(new (nothrow) VirtIONetworkAdapter(pci_device_identifier, interface_name_or_error.release_value()));
    adapter->initialize();

    return adapter;
}

UNMAP_AFTER_INIT void VirtIONetworkAdapter::initialize()
{
    dmesgln("VirtIO::NetworkAdaper: Found @ {}", pci_address());

    Device::initialize();
    if (auto* config = get_config(VirtIO::ConfigurationType::Device)) {
        m_device_configuration = config;
        bool success = negotiate_features([&](u64 supported_features) {
            u64 negotiated = 0;

            if (is_feature_set(supported_features, VIRTIO_NET_F_MAC)) {
                dbgln_if(VIRTIO_DEBUG, "VirtIO::NetworkAdaper: device accepts MAC");
                negotiated |= VIRTIO_NET_F_MAC;
            } else
                VERIFY_NOT_REACHED(); // TODO: handle setting the mac address

            if (is_feature_set(supported_features, VIRTIO_NET_F_STATUS)) {
                dbgln_if(VIRTIO_DEBUG, "VirtIO::NetworkAdaper: device accepts status");
                negotiated |= VIRTIO_NET_F_STATUS;
            }

            return negotiated;
        });
        VERIFY(success);

        // 5.1.5 If VIRTIO_NET_F_MQ feature bit is negotiated, N=max_virtqueue_pairs, otherwise identify N=1.
        success = setup_queues(1);
        VERIFY(success);
    } else {
        VERIFY_NOT_REACHED();
    }

    finish_init();

    read_mac_address();
    const auto& mac = mac_address();
    dmesgln("VirtIO::NetworkAdaper: MAC address: {}", mac.to_string());

    auto status = config_read16(*m_device_configuration, offsetof(struct virtio_net_config, status));
    dmesgln("VirtIO::NetworkAdaper: status: {}", status);

    // Set the initial link up status.
    m_link_up = (status & (1 << VIRTIO_NET_S_LINK_UP)) == 0;
}

UNMAP_AFTER_INIT VirtIONetworkAdapter::VirtIONetworkAdapter(PCI::DeviceIdentifier const& pci_device_identifier, NonnullOwnPtr<KString> interface_name)
    : NetworkAdapter(move(interface_name))
    , VirtIO::Device(pci_device_identifier)
    , m_packet_buffer(MM.allocate_contiguous_kernel_region(Memory::page_round_up(PACKET_SIZE_MAX).release_value_but_fixme_should_propagate_errors(), "VirtIO::NetworkAdaper Packet buffer", Memory::Region::Access::ReadWrite).release_value())
{
}

UNMAP_AFTER_INIT VirtIONetworkAdapter::~VirtIONetworkAdapter()
{
}

bool VirtIONetworkAdapter::handle_device_config_change()
{
    VERIFY_NOT_REACHED();
    return false;
}

void VirtIONetworkAdapter::handle_queue_update(u16)
{
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT void VirtIONetworkAdapter::read_mac_address()
{
    MACAddress mac {};
    for (int i = 0; i < 6; i++)
        mac[i] = config_read8(*m_device_configuration, offsetof(struct virtio_net_config, mac) + i);
    set_mac_address(mac);
}

void VirtIONetworkAdapter::send_raw(ReadonlyBytes payload)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIO::NetworkAdaper: send_raw length={}", payload.size());
    VERIFY(m_outstanding_request.is_empty());

    auto writer = create_packet_buffer_writer();
    auto& packet = writer.append_structure<Kernel::Net::VirtIO::Protocol::Packet>();
    packet.header.flags = 0;
    packet.header.gso_type = 0;
    packet.header.hdr_len = 0;
    packet.header.gso_size = 0;
    packet.header.csum_start = 0;
    packet.header.csum_offset = 0;

    auto& queue = get_queue(0);
    {
        SpinlockLocker lock(queue.lock());
        VirtIO::QueueChain chain { queue };
        chain.add_buffer_to_chain(start_of_packet_buffer(), sizeof(packet), VirtIO::BufferType::DeviceReadable);
        supply_chain_and_notify(0, chain);
        full_memory_barrier();
    }
    m_outstanding_request.wait_forever();
}

}
