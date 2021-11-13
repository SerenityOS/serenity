/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATA/AHCI.h>
#include <Kernel/Storage/ATA/ATAController.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AsyncBlockDeviceRequest;
class AHCIPortHandler;
class AHCIPort;
class AHCIController final : public ATAController
    , public PCI::Device {
    friend class AHCIPortHandler;
    friend class AHCIPort;
    AK_MAKE_ETERNAL
public:
    UNMAP_AFTER_INIT static NonnullRefPtr<AHCIController> initialize(PCI::DeviceIdentifier const& pci_device_identifier);
    virtual ~AHCIController() override;

    virtual RefPtr<StorageDevice> device(u32 index) const override;
    virtual bool reset() override;
    virtual bool shutdown() override;
    virtual size_t devices_count() const override;
    virtual void start_request(const ATADevice&, AsyncBlockDeviceRequest&) override;
    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) override;

    const AHCI::HBADefinedCapabilities& hba_capabilities() const { return m_capabilities; };

private:
    void disable_global_interrupts() const;
    void enable_global_interrupts() const;

    UNMAP_AFTER_INIT explicit AHCIController(PCI::DeviceIdentifier const&);
    UNMAP_AFTER_INIT void initialize_hba(PCI::DeviceIdentifier const&);

    AHCI::HBADefinedCapabilities capabilities() const;
    RefPtr<StorageDevice> device_by_port(u32 index) const;

    volatile AHCI::PortRegisters& port(size_t port_number) const;
    UNMAP_AFTER_INIT NonnullOwnPtr<Memory::Region> default_hba_region() const;
    volatile AHCI::HBA& hba() const;

    NonnullOwnPtr<Memory::Region> m_hba_region;
    AHCI::HBADefinedCapabilities m_capabilities;
    NonnullRefPtrVector<AHCIPortHandler> m_handlers;
};
}
