/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Net/Intel/E1000NetworkAdapter.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class E1000ENetworkAdapter final
    : public E1000NetworkAdapter {
public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullRefPtr<NetworkAdapter>> create(PCI::DeviceIdentifier const&);
    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) override;

    virtual ~E1000ENetworkAdapter() override;

    virtual StringView purpose() const override { return class_name(); }

private:
    E1000ENetworkAdapter(StringView interface_name, PCI::DeviceIdentifier const&, u8 irq,
        NonnullOwnPtr<IOWindow> registers_io_window, NonnullOwnPtr<Memory::Region> rx_buffer_region,
        NonnullOwnPtr<Memory::Region> tx_buffer_region, Memory::TypedMapping<RxDescriptor volatile[]> rx_descriptors,
        Memory::TypedMapping<TxDescriptor volatile[]> tx_descriptors);

    virtual StringView class_name() const override { return "E1000ENetworkAdapter"sv; }

    virtual void detect_eeprom() override;
    virtual u32 read_eeprom(u8 address) override;
};
}
