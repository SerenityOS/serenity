/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Storage/ATA/ATAController.h>
#include <Kernel/Storage/ATA/IDEChannel.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class IDEController final : public ATAController
    , public PCI::Device {
    AK_MAKE_ETERNAL
public:
    static NonnullRefPtr<IDEController> initialize(PCI::DeviceIdentifier const&, bool force_pio);
    virtual ~IDEController() override;

    virtual RefPtr<StorageDevice> device(u32 index) const override;
    virtual bool reset() override;
    virtual bool shutdown() override;
    virtual size_t devices_count() const override;
    virtual void start_request(const ATADevice&, AsyncBlockDeviceRequest&) override;
    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) override;

    bool is_bus_master_capable() const;
    bool is_pci_native_mode_enabled() const;

private:
    bool is_pci_native_mode_enabled_on_primary_channel() const;
    bool is_pci_native_mode_enabled_on_secondary_channel() const;
    IDEController(PCI::DeviceIdentifier const&, bool force_pio);

    RefPtr<StorageDevice> device_by_channel_and_position(u32 index) const;
    void initialize(bool force_pio);
    void detect_disks();

    NonnullRefPtrVector<IDEChannel> m_channels;
    // FIXME: Find a better way to get the ProgrammingInterface
    PCI::ProgrammingInterface m_prog_if;
    PCI::InterruptLine m_interrupt_line;
};
}
