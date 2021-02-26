/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>
#include <Kernel/Storage/AHCIController.h>
#include <Kernel/Storage/AHCIPort.h>
#include <Kernel/Storage/StorageDevice.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class AHCIController;
class AHCIPort;
class AHCIPortHandler final : public RefCounted<AHCIPortHandler>
    , public IRQHandler {
    friend class AHCIController;
    friend class SATADiskDevice;

public:
    UNMAP_AFTER_INIT static NonnullRefPtr<AHCIPortHandler> create(AHCIController&, u8 irq, AHCI::MaskedBitField taken_ports);
    virtual ~AHCIPortHandler() override;

    RefPtr<StorageDevice> device_at_port(size_t port_index) const;
    virtual const char* purpose() const override { return "SATA Port Handler"; }

    AHCI::HBADefinedCapabilities hba_capabilities() const;
    NonnullRefPtr<AHCIController> hba_controller() const { return m_parent_controller; }
    PhysicalAddress get_identify_metadata_physical_region(u32 port_index) const;

    bool is_responsible_for_port_index(u32 port_index) const { return m_taken_ports.is_set_at(port_index); }

private:
    UNMAP_AFTER_INIT AHCIPortHandler(AHCIController&, u8 irq, AHCI::MaskedBitField taken_ports);

    //^ IRQHandler
    virtual void handle_irq(const RegisterState&) override;

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
    NonnullRefPtrVector<PhysicalPage> m_identify_metadata_pages;
    AHCI::MaskedBitField m_taken_ports;
    AHCI::MaskedBitField m_pending_ports_interrupts;
};
}
