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
#include <AK/String.h>
#include <Kernel/Devices/AHCIDiskDevice.h>
#include <Kernel/VM/Region.h>

namespace Kernel {

class AHCIPort;
class AHCISlot {
public:
    AHCISlot() {}
    AHCISlot(VirtualAddress base, AHCIPort& port,
        VirtualAddress buffer, PhysicalAddress buffer_physical,
        u32 index);

    bool issue_identify(AHCIPort& port, u8* out);
    bool issue_read(AHCIPort& port, u32 start, u16 count, u8* out);
    u32 index() const { return m_index; }

private:
    void setup_command(AHCIPort& port);
    bool execute_command(AHCIPort& port);
    void rebase(AHCIPort& port);

    VirtualAddress m_base_addr;
    VirtualAddress m_buffer;
    PhysicalAddress m_buffer_physical;
    u32 m_index;
    u32 m_ctba_offset;

    struct DataEntry {
        u32 dba;
        u32 dbau;
        u32 rsv0;
        u32 dword3;
    };

    struct SlotReg {
        u16 word0;
        u16 prdtl;
        u32 prdbc;
        u32 ctba;
        u32 ctbau;
        u32 rsv1[4];
    };

    SlotReg* m_reg;
    u8* m_cfis;
    u8* m_acmd;
    DataEntry* m_entries;
};

class AHCIPort {
public:
    enum Type {
        SATA,
        SATAPI,
        SEMB,
        PM,
        UNKOWN
    };

    AHCIPort(VirtualAddress base, u32 index);
    bool handle_irq();

    void rebase();
    bool is_busy();
    bool is_executing(u32 slot);
    bool has_file_error();
    void issue_command(u32 slot);
    AHCISlot& find_free_slot();

    u32 is() { return m_reg->is; }
    u32 index() const { return m_index; }
    RefPtr<AHCIDiskDevice> disk_device() { return m_disk_device; }

    uintptr_t base() const { return m_ahci_base.get(); }
    uintptr_t base_physical() const { return m_ahci_base_physical.get(); }
    const Type& type() const { return m_type; }

private:
    Type check_type();
    void identify_device();
    void stop_command_execution();
    void start_command_execution();

    // Device mata info
    OwnPtr<Region> m_ahci_base_region;
    VirtualAddress m_ahci_base;
    PhysicalAddress m_ahci_base_physical;
    VirtualAddress m_base_addr;
    u32 m_index;
    Type m_type;
    RefPtr<AHCIDiskDevice> m_disk_device;

    OwnPtr<Region> m_buffer_region;
    VirtualAddress m_buffer;
    PhysicalAddress m_buffer_physical;
    u32 m_clb_offset;
    u32 m_fb_offset;

    struct PortReg {
        u32 clb;
        u32 clbu;
        u32 fb;
        u32 fbu;
        u32 is;
        u32 ie;
        u32 cmd;
        u32 rsv0;
        u32 tfd;
        u32 sig;
        u32 ssts;
        u32 sctl;
        u32 serr;
        u32 sact;
        u32 ci;
        u32 sntf;
        u32 fbs;
        u32 rsv1[11];
        u32 vendor[4];
    };

    // Data
    PortReg* m_reg;
    AHCISlot m_commands[32];
};

}
