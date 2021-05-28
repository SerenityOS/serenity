/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Storage/AHCI.h>
#include <Kernel/Storage/StorageController.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AsyncBlockDeviceRequest;
class AHCIPortHandler;
class AHCIPort;
class AHCIController final : public StorageController
    , public PCI::DeviceController {
    friend class AHCIPortHandler;
    friend class AHCIPort;
    AK_MAKE_ETERNAL
public:
public:
    UNMAP_AFTER_INIT static NonnullRefPtr<AHCIController> initialize(PCI::Address address);
    virtual ~AHCIController() override;

    virtual RefPtr<StorageDevice> device(u32 index) const override;
    virtual bool reset() override;
    virtual bool shutdown() override;
    virtual size_t devices_count() const override;
    virtual void start_request(const StorageDevice&, AsyncBlockDeviceRequest&) override;
    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) override;

    const AHCI::HBADefinedCapabilities& hba_capabilities() const { return m_capabilities; };

private:
    void disable_global_interrupts() const;
    void enable_global_interrupts() const;

    UNMAP_AFTER_INIT explicit AHCIController(PCI::Address address);
    UNMAP_AFTER_INIT void initialize();

    AHCI::HBADefinedCapabilities capabilities() const;
    RefPtr<StorageDevice> device_by_port(u32 index) const;

    volatile AHCI::PortRegisters& port(size_t port_number) const;
    UNMAP_AFTER_INIT NonnullOwnPtr<Region> default_hba_region() const;
    volatile AHCI::HBA& hba() const;

    NonnullOwnPtr<Region> m_hba_region;
    AHCI::HBADefinedCapabilities m_capabilities;
    NonnullRefPtrVector<AHCIPortHandler> m_handlers;
};
}
