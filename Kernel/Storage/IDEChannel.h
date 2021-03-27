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

class IDEController;
class IDEChannel : public RefCounted<IDEChannel>
    , public IRQHandler {
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

        IOAddressGroup(IOAddress io_base, IOAddress control_base)
            : m_io_base(io_base)
            , m_control_base(control_base)
            , m_bus_master_base()
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
        Optional<IOAddress> bus_master_base() const { return m_bus_master_base; }

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
        Optional<IOAddress> m_bus_master_base;
    };

public:
    static NonnullRefPtr<IDEChannel> create(const IDEController&, IOAddressGroup, ChannelType type);
    static NonnullRefPtr<IDEChannel> create(const IDEController&, u8 irq, IOAddressGroup, ChannelType type);
    virtual ~IDEChannel() override;

    RefPtr<StorageDevice> master_device() const;
    RefPtr<StorageDevice> slave_device() const;

    virtual const char* purpose() const override { return "PATA Channel"; }

    virtual bool is_dma_enabled() const { return false; }

private:
    void complete_current_request(AsyncDeviceRequest::RequestResult);
    void initialize();

protected:
    enum class LBAMode : u8 {
        None, // CHS
        TwentyEightBit,
        FortyEightBit,
    };

    enum class Direction : u8 {
        Read,
        Write,
    };

    IDEChannel(const IDEController&, IOAddressGroup, ChannelType type);
    IDEChannel(const IDEController&, u8 irq, IOAddressGroup, ChannelType type);
    //^ IRQHandler
    virtual void handle_irq(const RegisterState&) override;

    virtual void send_ata_io_command(LBAMode lba_mode, Direction direction) const;

    virtual void ata_read_sectors(bool, u16);
    virtual void ata_write_sectors(bool, u16);

    void detect_disks();
    String channel_type_string() const;

    void try_disambiguate_error();
    void wait_until_not_busy();

    void start_request(AsyncBlockDeviceRequest&, bool, u16);

    void clear_pending_interrupts() const;

    void ata_access(Direction, bool, u64, u8, u16);

    bool ata_do_read_sector();
    void ata_do_write_sector();

    // Data members
    ChannelType m_channel_type { ChannelType::Primary };

    volatile u8 m_device_error { 0 };
    EntropySource m_entropy_source;

    RefPtr<StorageDevice> m_master;
    RefPtr<StorageDevice> m_slave;

    RefPtr<AsyncBlockDeviceRequest> m_current_request;
    size_t m_current_request_block_index { 0 };
    bool m_current_request_flushing_cache { false };
    SpinLock<u8> m_request_lock;
    Lock m_lock { "IDEChannel" };

    IOAddressGroup m_io_group;
    NonnullRefPtr<IDEController> m_parent_controller;
};
}
