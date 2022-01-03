/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BinaryBufferWriter.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Bus/VirtIO/Queue.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/VirtIO/Protocol.h>
#include <Kernel/Random.h>

namespace Kernel {

// VirtIO Driver based on https://docs.oasis-open.org/virtio/virtio/v1.1/virtio-v1.1.html
class VirtIONetworkAdapter final
    : public NetworkAdapter
    , public VirtIO::Device {
public:
    static RefPtr<VirtIONetworkAdapter> try_to_initialize(PCI::DeviceIdentifier const&);

    virtual void initialize() override;

    virtual ~VirtIONetworkAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual bool link_up() override { return m_link_up; };

    virtual StringView purpose() const override { return class_name(); }

private:
    VirtIONetworkAdapter(PCI::DeviceIdentifier const&, NonnullOwnPtr<KString>);

    virtual StringView class_name() const override { return "VirtIONetworkAdapter"sv; }

    struct [[gnu::packed]] virtio_net_config {
        volatile uint8_t mac[6] { 0 };
        volatile uint16_t status { 0 };
        volatile uint16_t max_virtqueue_pairs { 0 };
        volatile uint16_t mtu { 0 };
    };

    PhysicalAddress start_of_packet_buffer() const { return m_packet_buffer->physical_page(0)->paddr(); }
    AK::BinaryBufferWriter create_packet_buffer_writer()
    {
        return { Bytes(m_packet_buffer->vaddr().as_ptr(), m_packet_buffer->size()) };
    }
    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;

    void read_mac_address();

    void receive();

    VirtIO::Configuration const* m_device_configuration { nullptr };
    WaitQueue m_outstanding_request;
    OwnPtr<Memory::Region> m_packet_buffer;
    bool m_link_up { false };
};
}
