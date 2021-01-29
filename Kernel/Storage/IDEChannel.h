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
#include <Kernel/Devices/Device.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>
#include <Kernel/Storage/StorageDevice.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

struct PhysicalRegionDescriptor {
    PhysicalAddress offset;
    u16 size { 0 };
    u16 end_of_table { 0 };
};

class IDEController;
class IDEChannel final : public IRQHandler {
    friend class IDEController;
    friend class PATADiskDevice;
    AK_MAKE_ETERNAL
public:
    enum class ChannelType : u8 {
        Primary,
        Secondary
    };

    struct IOAddressGroup {
        IOAddressGroup(IOAddress io_base, IOAddress control_base, IOAddress bus_master_base)
            : m_io_base(io_base)
            , m_control_base(control_base)
            , m_bus_master_base(bus_master_base)
        {
        }

        // Disable default implementations that would use surprising integer promotion.
        bool operator==(const IOAddressGroup&) const = delete;
        bool operator<=(const IOAddressGroup&) const = delete;
        bool operator>=(const IOAddressGroup&) const = delete;
        bool operator<(const IOAddressGroup&) const = delete;
        bool operator>(const IOAddressGroup&) const = delete;

        IOAddress io_base() const { return m_io_base; };
        IOAddress control_base() const { return m_control_base; }
        IOAddress bus_master_base() const { return m_bus_master_base; }

        const IOAddressGroup& operator=(const IOAddressGroup& group)
        {
            m_io_base = group.io_base();
            m_control_base = group.control_base();
            m_bus_master_base = group.bus_master_base();
            return *this;
        }

    private:
        IOAddress m_io_base;
        IOAddress m_control_base;
        IOAddress m_bus_master_base;
    };

public:
    static NonnullOwnPtr<IDEChannel> create(const IDEController&, IOAddressGroup, ChannelType type, bool force_pio);
    IDEChannel(const IDEController&, IOAddressGroup, ChannelType type, bool force_pio);
    virtual ~IDEChannel() override;

    RefPtr<StorageDevice> master_device() const;
    RefPtr<StorageDevice> slave_device() const;

    virtual const char* purpose() const override { return "PATA Channel"; }

private:
    //^ IRQHandler
    virtual void handle_irq(const RegisterState&) override;

    enum class LBAMode : u8 {
        None, // CHS
        TwentyEightBit,
        FortyEightBit,
    };

    enum class Direction : u8 {
        Read,
        Write,
    };

    void initialize(bool force_pio);
    void detect_disks();
    String channel_type_string() const;

    void try_disambiguate_error();
    void wait_until_not_busy();

    void start_request(AsyncBlockDeviceRequest&, bool, bool, u16);
    void complete_current_request(AsyncDeviceRequest::RequestResult);

    void ata_access(Direction, bool, u32, u8, u16, bool);
    void ata_read_sectors_with_dma(bool, u16);
    void ata_read_sectors(bool, u16);
    bool ata_do_read_sector();
    void ata_write_sectors_with_dma(bool, u16);
    void ata_write_sectors(bool, u16);
    void ata_do_write_sector();

    // Data members
    ChannelType m_channel_type { ChannelType::Primary };

    volatile u8 m_device_error { 0 };

    PhysicalRegionDescriptor& prdt() { return *reinterpret_cast<PhysicalRegionDescriptor*>(m_prdt_page->paddr().offset(0xc0000000).as_ptr()); }
    RefPtr<PhysicalPage> m_prdt_page;
    RefPtr<PhysicalPage> m_dma_buffer_page;
    Lockable<bool> m_dma_enabled;
    EntropySource m_entropy_source;

    RefPtr<StorageDevice> m_master;
    RefPtr<StorageDevice> m_slave;

    AsyncBlockDeviceRequest* m_current_request { nullptr };
    u32 m_current_request_block_index { 0 };
    bool m_current_request_uses_dma { false };
    bool m_current_request_flushing_cache { false };
    SpinLock<u8> m_request_lock;

    IOAddressGroup m_io_group;
    NonnullRefPtr<IDEController> m_parent_controller;
};
}
