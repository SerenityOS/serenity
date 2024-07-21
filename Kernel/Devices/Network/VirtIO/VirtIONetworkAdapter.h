/*
 * Copyright (c) 2023, Kirill Nikolaev <cyril7@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Memory/RingBuffer.h>
#include <Kernel/Net/NetworkAdapter.h>

namespace Kernel {

class VirtIONetworkAdapter
    : public VirtIO::Device
    , public NetworkAdapter {

public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullRefPtr<NetworkAdapter>> create(PCI::DeviceIdentifier const&);
    virtual ~VirtIONetworkAdapter() override = default;

    // VirtIO::Device
    virtual ErrorOr<void> initialize_virtio_resources() override;

    // NetworkAdapter
    virtual StringView class_name() const override { return "VirtIONetworkAdapter"sv; }
    virtual Type adapter_type() const override { return Type::Ethernet; }
    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) override;

    virtual bool link_up() override { return m_link_up; }
    virtual bool link_full_duplex() override { return m_link_duplex; }
    virtual i32 link_speed() override { return m_link_speed; }

private:
    explicit VirtIONetworkAdapter(StringView interface_name, NonnullOwnPtr<VirtIO::TransportEntity>);

    // VirtIO::Device
    virtual ErrorOr<void> handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;

    // NetworkAdapter
    virtual void send_raw(ReadonlyBytes) override;

private:
    VirtIO::Configuration const* m_device_config { nullptr };

    // FIXME: Make atomic as they are read without sync.
    // Note that VirtIO::NetworkAdapter may also have the same defect.
    bool m_link_up { false };
    i32 m_link_speed { LINKSPEED_INVALID };
    bool m_link_duplex { false };

    OwnPtr<Memory::RingBuffer> m_rx_buffers;
    OwnPtr<Memory::RingBuffer> m_tx_buffers;
};

}
