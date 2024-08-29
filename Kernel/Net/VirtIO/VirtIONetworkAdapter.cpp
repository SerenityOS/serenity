/*
 * Copyright (c) 2023, Kirill Nikolaev <cyril7@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/VirtIO/VirtIONetworkAdapter.h>

namespace Kernel {

namespace VirtIO {

// https://docs.oasis-open.org/virtio/virtio/v1.2/csd01/virtio-v1.2-csd01.html

static constexpr u64 VIRTIO_NET_F_CSUM = (1ull << 0);                // Device handles packets with partial checksum.
static constexpr u64 VIRTIO_NET_F_GUEST_CSUM = (1ull << 1);          // Driver handles packets with partial checksum.
static constexpr u64 VIRTIO_NET_F_CTRL_GUEST_OFFLOADS = (1ull << 2); // Control channel offloads reconfiguration support.
static constexpr u64 VIRTIO_NET_F_MTU = (1ull << 3);                 // Device maximum MTU reporting is supported.
static constexpr u64 VIRTIO_NET_F_MAC = (1ull << 5);                 // Device has given MAC address.
static constexpr u64 VIRTIO_NET_F_GUEST_TSO4 = (1ull << 7);          // Driver can receive TSOv4.
static constexpr u64 VIRTIO_NET_F_GUEST_TSO6 = (1ull << 8);          // Driver can receive TSOv6.
static constexpr u64 VIRTIO_NET_F_GUEST_ECN = (1ull << 9);           // Driver can receive TSO with ECN.
static constexpr u64 VIRTIO_NET_F_GUEST_UFO = (1ull << 10);          // Driver can receive UFO.
static constexpr u64 VIRTIO_NET_F_HOST_TSO4 = (1ull << 11);          // Device can receive TSOv4.
static constexpr u64 VIRTIO_NET_F_HOST_TSO6 = (1ull << 12);          // Device can receive TSOv6.
static constexpr u64 VIRTIO_NET_F_HOST_ECN = (1ull << 13);           // Device can receive TSO with ECN.
static constexpr u64 VIRTIO_NET_F_HOST_UFO = (1ull << 14);           // Device can receive UFO.
static constexpr u64 VIRTIO_NET_F_MRG_RXBUF = (1ull << 15);          // Driver can merge receive buffers.
static constexpr u64 VIRTIO_NET_F_STATUS = (1ull << 16);             // Configuration status field is available.
static constexpr u64 VIRTIO_NET_F_CTRL_VQ = (1ull << 17);            // Control channel is available.
static constexpr u64 VIRTIO_NET_F_CTRL_RX = (1ull << 18);            // Control channel RX mode support.
static constexpr u64 VIRTIO_NET_F_CTRL_VLAN = (1ull << 19);          // Control channel VLAN filtering.
static constexpr u64 VIRTIO_NET_F_GUEST_ANNOUNCE = (1ull << 21);     // Driver can send gratuitous packets.
static constexpr u64 VIRTIO_NET_F_MQ = (1ull << 22);                 // Device supports multiqueue with automatic receive steering.
static constexpr u64 VIRTIO_NET_F_CTRL_MAC_ADDR = (1ull << 23);      // Set MAC address through control channel.
static constexpr u64 VIRTIO_NET_F_HOST_USO = (1ull << 56);           // Device can receive USO packets.
static constexpr u64 VIRTIO_NET_F_HASH_REPORT = (1ull << 57);        // Device can report per-packet hash value and a type of calculated hash.
static constexpr u64 VIRTIO_NET_F_GUEST_HDRLEN = (1ull << 59);       // Driver can provide the exact hdr_len value.
static constexpr u64 VIRTIO_NET_F_RSS = (1ull << 60);                // Device supports RSS with Toeplitz hash calculation
static constexpr u64 VIRTIO_NET_F_RSC_EXT = (1ull << 21);            // Device can process duplicated ACKs and report number of coalesced segments and duplicated ACKs.
static constexpr u64 VIRTIO_NET_F_STANDBY = (1ull << 63);            // Device may act as a standby for a primary device with the same MAC address.
static constexpr u64 VIRTIO_NET_F_SPEED_DUPLEX = (1ull << 63);       // Device reports speed and duplex.

static constexpr u16 VIRTIO_NET_S_LINK_UP = 1;
static constexpr u16 VIRTIO_NET_S_ANNOUNCE = 2;

static constexpr u8 VIRTIO_NET_HDR_F_NEEDS_CSUM = 1;
static constexpr u8 VIRTIO_NET_HDR_F_DATA_VALID = 1;
static constexpr u8 VIRTIO_NET_HDR_F_RSC_INFO = 1;
static constexpr u8 VIRTIO_NET_HDR_GSO_NONE = 0;
static constexpr u8 VIRTIO_NET_HDR_GSO_TCPV4 = 1;
static constexpr u8 VIRTIO_NET_HDR_GSO_UDP = 3;
static constexpr u8 VIRTIO_NET_HDR_GSO_TCPV6 = 4;
static constexpr u8 VIRTIO_NET_HDR_GSO_UDP_L4 = 5;
static constexpr u8 VIRTIO_NET_HDR_GSO_ECN = 0x80;

struct [[gnu::packed]] VirtIONetConfig {
    u8 mac[6];
    LittleEndian<u16> status;
    LittleEndian<u16> max_virtqueue_pairs;
    LittleEndian<u16> mtu;
    LittleEndian<u32> speed;
    u8 duplex;
    u8 rss_max_key_size;
    LittleEndian<u16> rss_max_indirection_table_length;
    LittleEndian<u32> supported_hash_types;
};

struct [[gnu::packed]] VirtIONetHdr {
    u8 flags;
    u8 gso_type;
    LittleEndian<u16> hdr_len;
    LittleEndian<u16> gso_size;
    LittleEndian<u16> csum_start;
    LittleEndian<u16> csum_offset;
    LittleEndian<u16> num_buffers;
    u8 frame[0];
};

}

using namespace VirtIO;

static constexpr u16 RECEIVEQ = 0;
static constexpr u16 TRANSMITQ = 1;

static constexpr size_t MAX_RX_FRAME_SIZE = 1514; // Non-jumbo Ethernet frame limit.
static constexpr size_t RX_BUFFER_SIZE = sizeof(VirtIONetHdr) * MAX_RX_FRAME_SIZE;
static constexpr u16 MAX_INFLIGHT_PACKETS = 128;

UNMAP_AFTER_INIT ErrorOr<bool> VirtIONetworkAdapter::probe(PCI::DeviceIdentifier const& pci_device_identifier)
{
    if (pci_device_identifier.hardware_id().vendor_id != PCI::VendorID::VirtIO)
        return false;
    if (pci_device_identifier.hardware_id().device_id != PCI::DeviceID::VirtIONetAdapter)
        return false;
    return true;
}

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NetworkAdapter>> VirtIONetworkAdapter::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto interface_name = TRY(NetworkingManagement::generate_interface_name_from_pci_address(pci_device_identifier));
    auto pci_transport_link = TRY(VirtIO::PCIeTransportLink::create(pci_device_identifier));
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) VirtIONetworkAdapter(interface_name.representable_view(), move(pci_transport_link))));
}

UNMAP_AFTER_INIT VirtIONetworkAdapter::VirtIONetworkAdapter(StringView interface_name, NonnullOwnPtr<VirtIO::TransportEntity> pci_transport_link)
    : VirtIO::Device(move(pci_transport_link))
    , NetworkAdapter(interface_name)
{
}

UNMAP_AFTER_INIT ErrorOr<void> VirtIONetworkAdapter::initialize(Badge<NetworkingManagement>)
{
    m_rx_buffers = TRY(Memory::RingBuffer::try_create("VirtIONetworkAdapter Rx buffer"sv, RX_BUFFER_SIZE * MAX_INFLIGHT_PACKETS));
    m_tx_buffers = TRY(Memory::RingBuffer::try_create("VirtIONetworkAdapter Tx buffer"sv, RX_BUFFER_SIZE * MAX_INFLIGHT_PACKETS));

    return initialize_virtio_resources();
}

UNMAP_AFTER_INIT ErrorOr<void> VirtIONetworkAdapter::initialize_virtio_resources()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIONetworkAdapter: initialize_virtio_resources");
    TRY(Device::initialize_virtio_resources());
    m_device_config = TRY(transport_entity().get_config(VirtIO::ConfigurationType::Device));

    TRY(negotiate_features([&](u64 supported_features) {
        u64 negotiated = 0;
        if (is_feature_set(supported_features, VIRTIO_NET_F_STATUS))
            negotiated |= VIRTIO_NET_F_STATUS;
        if (is_feature_set(supported_features, VIRTIO_NET_F_MAC))
            negotiated |= VIRTIO_NET_F_MAC;
        if (is_feature_set(supported_features, VIRTIO_NET_F_SPEED_DUPLEX))
            negotiated |= VIRTIO_NET_F_SPEED_DUPLEX;
        if (is_feature_set(supported_features, VIRTIO_NET_F_MTU))
            negotiated |= VIRTIO_NET_F_MTU;
        return negotiated;
    }));

    TRY(handle_device_config_change());
    TRY(setup_queues(2)); // receive & transmit

    finish_init();

    {
        // Supply receive buffers.
        auto& rx_queue = get_queue(RECEIVEQ);
        SpinlockLocker queue_lock(rx_queue.lock());
        VirtIO::QueueChain chain(rx_queue);
        while (m_rx_buffers->available_bytes() > RX_BUFFER_SIZE) {
            // We know that the RingBuffer will not wraparound in this loop. But it's still awkward.
            auto buffer_start = MUST(m_rx_buffers->reserve_space(RX_BUFFER_SIZE));
            VERIFY(chain.add_buffer_to_chain(buffer_start, RX_BUFFER_SIZE, VirtIO::BufferType::DeviceWritable));
            supply_chain_and_notify(RECEIVEQ, chain);
        }
    }

    return {};
}

ErrorOr<void> VirtIONetworkAdapter::handle_device_config_change()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIONetworkAdapter: handle_device_config_change");
    transport_entity().read_config_atomic([&]() {
        if (is_feature_accepted(VIRTIO_NET_F_MAC)) {
            set_mac_address(MACAddress(
                transport_entity().config_read8(*m_device_config, 0x0),
                transport_entity().config_read8(*m_device_config, 0x1),
                transport_entity().config_read8(*m_device_config, 0x2),
                transport_entity().config_read8(*m_device_config, 0x3),
                transport_entity().config_read8(*m_device_config, 0x4),
                transport_entity().config_read8(*m_device_config, 0x5)));
        }
        if (is_feature_accepted(VIRTIO_NET_F_STATUS)) {
            u16 status = transport_entity().config_read16(*m_device_config, offsetof(VirtIONetConfig, status));
            m_link_up = (status & VIRTIO_NET_S_LINK_UP) != 0;
        }
        if (is_feature_accepted(VIRTIO_NET_F_MTU)) {
            u16 mtu = transport_entity().config_read16(*m_device_config, offsetof(VirtIONetConfig, mtu));
            set_mtu(mtu);
        }
        if (is_feature_accepted(VIRTIO_NET_F_SPEED_DUPLEX)) {
            u32 speed = transport_entity().config_read32(*m_device_config, offsetof(VirtIONetConfig, speed));
            m_link_speed = speed;
            u32 duplex = transport_entity().config_read32(*m_device_config, offsetof(VirtIONetConfig, duplex));
            m_link_duplex = duplex == 0x01;
        }
    });
    autoconfigure_link_local_ipv6();
    return {};
}

void VirtIONetworkAdapter::handle_queue_update(u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIONetworkAdapter: handle_queue_update {}", queue_index);

    if (queue_index == RECEIVEQ) {
        // FIXME: Disable interrupts while receiving as recommended by the spec.
        auto& queue = get_queue(RECEIVEQ);
        SpinlockLocker queue_lock(queue.lock());
        size_t used;
        VirtIO::QueueChain popped_chain = queue.pop_used_buffer_chain(used);

        while (!popped_chain.is_empty()) {
            VERIFY(popped_chain.length() == 1);
            popped_chain.for_each([&](PhysicalAddress addr, size_t length) {
                size_t offset = addr.as_ptr() - m_rx_buffers->start_of_region().as_ptr();
                auto* message = reinterpret_cast<VirtIONetHdr*>(m_rx_buffers->vaddr().offset(offset).as_ptr());
                did_receive({ message->frame, length - sizeof(VirtIONetHdr) });
            });

            supply_chain_and_notify(RECEIVEQ, popped_chain);
            popped_chain = queue.pop_used_buffer_chain(used);
        }
    } else if (queue_index == TRANSMITQ) {
        auto& queue = get_queue(TRANSMITQ);
        SpinlockLocker queue_lock(queue.lock());
        SpinlockLocker ringbuffer_lock(m_tx_buffers->lock());

        size_t used;
        VirtIO::QueueChain popped_chain = queue.pop_used_buffer_chain(used);
        do {
            popped_chain.for_each([this](PhysicalAddress address, size_t length) {
                m_tx_buffers->reclaim_space(address, length);
            });
            popped_chain.release_buffer_slots_to_queue();
            popped_chain = queue.pop_used_buffer_chain(used);
        } while (!popped_chain.is_empty());
    } else {
        dmesgln("VirtIONetworkAdapter: unexpected update for queue {}", queue_index);
    }
}

static bool copy_data_to_chain(VirtIO::QueueChain& chain, Memory::RingBuffer& ring, u8 const* data, size_t length)
{
    UserOrKernelBuffer buf = UserOrKernelBuffer::for_kernel_buffer(const_cast<u8*>(data));

    size_t offset = 0;
    while (offset < length) {
        PhysicalAddress start_of_chunk;
        size_t length_of_chunk;
        VERIFY(ring.copy_data_in(buf, offset, length - offset, start_of_chunk, length_of_chunk));
        if (!chain.add_buffer_to_chain(start_of_chunk, length_of_chunk, VirtIO::BufferType::DeviceReadable)) {
            // FIXME: Rewind the RingBuffer.
            // We are leaving the RingBuffer in an inconsistent state, but interface doesn't allow to undo pushes :(.
            return false;
        }
        offset += length_of_chunk;
    }
    return true;
}

void VirtIONetworkAdapter::send_raw(ReadonlyBytes payload)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIONetworkAdapter: send_raw length={}", payload.size());

    auto& queue = get_queue(TRANSMITQ);
    SpinlockLocker queue_lock(queue.lock());
    VirtIO::QueueChain chain(queue);

    SpinlockLocker ringbuffer_lock(m_tx_buffers->lock());
    if (m_tx_buffers->available_bytes() < sizeof(VirtIONetHdr) + payload.size()) {
        // We can drop packets that don't fit to apply back pressure on eager senders.
        dmesgln("VirtIONetworkAdapter: not enough space in the buffer. Dropping packet");
        return;
    }

    // FIXME: Handle errors from pushing to the chain and rewind the RingBuffer.
    VirtIONetHdr hdr {};
    VERIFY(copy_data_to_chain(chain, *m_tx_buffers, reinterpret_cast<u8*>(&hdr), sizeof(hdr)));
    VERIFY(copy_data_to_chain(chain, *m_tx_buffers, payload.data(), payload.size()));

    supply_chain_and_notify(TRANSMITQ, chain);
}

}
