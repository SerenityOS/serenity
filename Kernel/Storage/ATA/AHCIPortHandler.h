/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATA/AHCIController.h>
#include <Kernel/Storage/ATA/AHCIPort.h>
#include <Kernel/Storage/StorageDevice.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class AHCIController;
class AHCIPort;
class AHCIPortHandler final : public RefCounted<AHCIPortHandler>
    , public IRQHandler {
    friend class AHCIController;

public:
    UNMAP_AFTER_INIT static NonnullRefPtr<AHCIPortHandler> create(AHCIController&, u8 irq, AHCI::MaskedBitField taken_ports);
    virtual ~AHCIPortHandler() override;

    RefPtr<StorageDevice> device_at_port(size_t port_index) const;
    virtual StringView purpose() const override { return "SATA Port Handler"sv; }

    AHCI::HBADefinedCapabilities hba_capabilities() const;
    NonnullRefPtr<AHCIController> hba_controller() const { return m_parent_controller; }
    PhysicalAddress get_identify_metadata_physical_region(u32 port_index) const;

    bool is_responsible_for_port_index(u32 port_index) const { return m_taken_ports.is_set_at(port_index); }

private:
    UNMAP_AFTER_INIT AHCIPortHandler(AHCIController&, u8 irq, AHCI::MaskedBitField taken_ports);

    //^ IRQHandler
    virtual bool handle_irq(const RegisterState&) override;

    enum class Direction : u8 {
        Read,
        Write,
    };

    AHCI::MaskedBitField create_pending_ports_interrupts_bitfield() const;

    void start_request(AsyncBlockDeviceRequest&, bool, bool, u16);
    void complete_current_request(AsyncDeviceRequest::RequestResult);

    void enumerate_ports(Function<void(const AHCIPort&)> callback) const;
    RefPtr<AHCIPort> port_at_index(u32 port_index) const;

    // Data members
    HashMap<u32, NonnullRefPtr<AHCIPort>> m_handled_ports;
    NonnullRefPtr<AHCIController> m_parent_controller;
    NonnullRefPtrVector<Memory::PhysicalPage> m_identify_metadata_pages;
    AHCI::MaskedBitField m_taken_ports;
    AHCI::MaskedBitField m_pending_ports_interrupts;
};
}
