/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>
#include <Kernel/Storage/ATA/ATADevice.h>
#include <Kernel/Storage/StorageDevice.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class IDEController;
class IDEChannel : public RefCounted<IDEChannel>
    , public IRQHandler {
    friend class IDEController;
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

        IOAddressGroup(IOAddressGroup const& other, IOAddress bus_master_base)
            : m_io_base(other.io_base())
            , m_control_base(other.control_base())
            , m_bus_master_base(bus_master_base)
        {
        }

        IOAddressGroup(IOAddressGroup const&) = default;

        // Disable default implementations that would use surprising integer promotion.
        bool operator==(const IOAddressGroup&) const = delete;
        bool operator<=(const IOAddressGroup&) const = delete;
        bool operator>=(const IOAddressGroup&) const = delete;
        bool operator<(const IOAddressGroup&) const = delete;
        bool operator>(const IOAddressGroup&) const = delete;

        IOAddress io_base() const { return m_io_base; };
        IOAddress control_base() const { return m_control_base; }
        Optional<IOAddress> bus_master_base() const { return m_bus_master_base; }

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

    virtual StringView purpose() const override { return "PATA Channel"sv; }

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
    virtual bool handle_irq(const RegisterState&) override;

    virtual void send_ata_io_command(LBAMode lba_mode, Direction direction) const;

    virtual void ata_read_sectors(bool, u16);
    virtual void ata_write_sectors(bool, u16);

    void detect_disks();
    StringView channel_type_string() const;

    void try_disambiguate_error();
    bool wait_until_not_busy(bool slave, size_t milliseconds_timeout);
    bool wait_until_not_busy(size_t milliseconds_timeout);

    void start_request(AsyncBlockDeviceRequest&, bool, u16);

    void clear_pending_interrupts() const;

    void ata_access(Direction, bool, u64, u8, u16);

    bool ata_do_read_sector();
    void ata_do_write_sector();

    // Data members
    ChannelType m_channel_type { ChannelType::Primary };

    volatile u8 m_device_error { 0 };
    EntropySource m_entropy_source;

    RefPtr<ATADevice> m_master;
    RefPtr<ATADevice> m_slave;

    RefPtr<AsyncBlockDeviceRequest> m_current_request;
    u64 m_current_request_block_index { 0 };
    bool m_current_request_flushing_cache { false };
    Spinlock m_request_lock;
    Mutex m_lock { "IDEChannel" };

    IOAddressGroup m_io_group;
    NonnullRefPtr<IDEController> m_parent_controller;
};
}
