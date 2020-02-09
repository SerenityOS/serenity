/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

//
// Parallel ATA (PATA) controller driver
//
// This driver describes a logical PATA Channel. Each channel can connect up to 2
// IDE Hard Disk Drives. The drives themselves can be either the master drive (hd0)
// or the slave drive (hd1).
//
// More information about the ATA spec for PATA can be found here:
//      ftp://ftp.seagate.com/acrobat/reference/111-1c.pdf
//
#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/WaitQueue.h>
#include <LibBareMetal/Memory/PhysicalAddress.h>

struct PhysicalRegionDescriptor {
    PhysicalAddress offset;
    u16 size { 0 };
    u16 end_of_table { 0 };
};

class PATADiskDevice;
class PATAChannel final : public IRQHandler {
    friend class PATADiskDevice;
    AK_MAKE_ETERNAL
public:
    enum class ChannelType : u8 {
        Primary,
        Secondary
    };

public:
    static OwnPtr<PATAChannel> create(ChannelType type, bool force_pio);
    PATAChannel(ChannelType type, bool force_pio);
    virtual ~PATAChannel() override;

    RefPtr<PATADiskDevice> master_device() { return m_master; };
    RefPtr<PATADiskDevice> slave_device() { return m_slave; };

private:
    //^ IRQHandler
    virtual void handle_irq() override;

    void initialize(bool force_pio);
    void detect_disks();

    void wait_for_irq();
    bool ata_read_sectors_with_dma(u32, u16, u8*, bool);
    bool ata_write_sectors_with_dma(u32, u16, const u8*, bool);
    bool ata_read_sectors(u32, u16, u8*, bool);
    bool ata_write_sectors(u32, u16, const u8*, bool);

    // Data members
    u8 m_channel_number { 0 }; // Channel number. 0 = master, 1 = slave
    u16 m_io_base { 0x1F0 };
    u16 m_control_base { 0 };
    volatile u8 m_device_error { 0 };

    WaitQueue m_irq_queue;

    PCI::Address m_pci_address;
    PhysicalRegionDescriptor& prdt() { return *reinterpret_cast<PhysicalRegionDescriptor*>(m_prdt_page->paddr().offset(0xc0000000).as_ptr()); }
    RefPtr<PhysicalPage> m_prdt_page;
    RefPtr<PhysicalPage> m_dma_buffer_page;
    u16 m_bus_master_base { 0 };
    Lockable<bool> m_dma_enabled;

    RefPtr<PATADiskDevice> m_master;
    RefPtr<PATADiskDevice> m_slave;
};
