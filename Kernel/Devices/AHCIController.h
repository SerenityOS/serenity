/*
* Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
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
#include <Kernel/Devices/AHCIDiskDevice.h>
#include <Kernel/Devices/AHCIPort.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/Device.h>
#include <Kernel/VM/Region.h>
#include <Kernel/WaitQueue.h>

//#define DEBUG_AHCI

namespace Kernel {

class AHCIController final : public PCI::Device {
    AK_MAKE_ETERNAL
public:
    static void create();
    static AHCIController& the();

    AHCIController(PCI::Address address);
    inline bool has_fatal_error() const { return m_has_fatal_error; }
    inline RefPtr<AHCIDiskDevice> first_device();

    virtual const char* purpose() const override { return "AHCI Controller"; }

    virtual ~AHCIController() override {}

private:
    static constexpr u32 m_port_count = 32;

    virtual void handle_irq(const RegisterState&) override;
    void initialize();
    void probe_ports();
    void fatal_error(u32 port_index);

    // Address space and mappings
    OwnPtr<Region> m_abar_region;
    VirtualAddress m_abar;
    PhysicalAddress m_base_physical;
    bool m_has_fatal_error;

    struct ABARReg {
        u32 cap;
        u32 ghc;
        u32 is;
        u32 pi;
        u32 vs;
        u32 ccc_ctl;
        u32 ccc_ports;
        u32 em_loc;
        u32 em_ctl;
        u32 cap2;
        u32 bohc;
    };

    // ABAR data pointers
    ABARReg* m_reg;
    OwnPtr<AHCIPort> m_ports[m_port_count];
};

}
