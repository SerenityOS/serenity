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
#include <Kernel/Net/MDIO.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class E1000ENetworkAdapter final
    : public E1000NetworkAdapter
    , public MDIO::Clause22::Interface {
public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullRefPtr<NetworkAdapter>> create(PCI::DeviceIdentifier const&);
    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) override;

    virtual ~E1000ENetworkAdapter() override;

protected:
    // ^MDIO::Clause22::Interface
    virtual u16 read_phy_register(u8 phy_id, MDIO::Clause22::RegisterAddress address) override;
    virtual void write_phy_register(u8 phy_id, MDIO::Clause22::RegisterAddress address, u16 value) override;

private:
    E1000ENetworkAdapter(StringView interface_name, PCI::DeviceIdentifier const&,
        NonnullOwnPtr<IOWindow> registers_io_window, NonnullOwnPtr<Memory::Region> rx_buffer_region,
        NonnullOwnPtr<Memory::Region> tx_buffer_region, Memory::TypedMapping<RxDescriptor volatile[]> rx_descriptors,
        Memory::TypedMapping<TxDescriptor volatile[]> tx_descriptors);

    virtual StringView class_name() const override { return "E1000ENetworkAdapter"sv; }

    RefPtr<Process> m_mdio_handling_process;
};
}
