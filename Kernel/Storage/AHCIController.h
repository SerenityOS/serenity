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

    virtual Type type() const override { return Type::AHCI; }
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
    NonnullOwnPtr<Region> hba_region() const;
    volatile AHCI::HBA& hba() const;

    NonnullOwnPtr<Region> m_hba_region;
    AHCI::HBADefinedCapabilities m_capabilities;
    NonnullRefPtrVector<AHCIPortHandler> m_handlers;
};
}
