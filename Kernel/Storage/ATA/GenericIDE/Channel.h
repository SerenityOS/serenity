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

#include <AK/Badge.h>
#include <AK/RefPtr.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>
#include <Kernel/Storage/ATA/ATADevice.h>
#include <Kernel/Storage/ATA/GenericIDE/Controller.h>
#include <Kernel/Storage/StorageDevice.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class PCIIDEController;
class ISAIDEController;
class IDEChannel : public RefCounted<IDEChannel>
    , public IRQHandler {
    friend class IDEController;

public:
    enum class ChannelType : u8 {
        Primary,
        Secondary
    };

    enum class DeviceType : u8 {
        Master,
        Slave,
    };

    struct IOAddressGroup {
        IOAddressGroup(IOAddress io_base, IOAddress control_base, IOAddress bus_master_base)
            : m_io_base(io_base)
            , m_control_base(control_base)
            , m_bus_master_base(bus_master_base)
        {
        }

        IOAddressGroup(IOAddress io_base, IOAddress control_base, Optional<IOAddress> bus_master_base)
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
        bool operator==(IOAddressGroup const&) const = delete;
        bool operator<=(IOAddressGroup const&) const = delete;
        bool operator>=(IOAddressGroup const&) const = delete;
        bool operator<(IOAddressGroup const&) const = delete;
        bool operator>(IOAddressGroup const&) const = delete;

        IOAddress io_base() const { return m_io_base; };
        IOAddress control_base() const { return m_control_base; }
        Optional<IOAddress> bus_master_base() const { return m_bus_master_base; }

    private:
        IOAddress m_io_base;
        IOAddress m_control_base;
        Optional<IOAddress> m_bus_master_base;
    };

public:
    static NonnullRefPtr<IDEChannel> create(IDEController const&, IOAddressGroup, ChannelType type);
    static NonnullRefPtr<IDEChannel> create(IDEController const&, u8 irq, IOAddressGroup, ChannelType type);

    void initialize_with_pci_controller(Badge<PCIIDEController>, bool force_pio);
    void initialize_with_isa_controller(Badge<ISAIDEController>, bool force_pio);
    virtual ~IDEChannel() override;

    RefPtr<StorageDevice> master_device() const;
    RefPtr<StorageDevice> slave_device() const;

    virtual StringView purpose() const override { return "PATA Channel"sv; }

private:
    static constexpr size_t m_logical_sector_size = 512;
    void initialize(bool force_pio);
    struct [[gnu::packed]] PhysicalRegionDescriptor {
        u32 offset;
        u16 size { 0 };
        u16 end_of_table { 0 };
    };

    enum class LBAMode : u8 {
        None, // CHS
        TwentyEightBit,
        FortyEightBit,
    };

    enum class Direction : u8 {
        Read,
        Write,
    };

    IDEChannel(IDEController const&, IOAddressGroup, ChannelType type);
    IDEChannel(IDEController const&, u8 irq, IOAddressGroup, ChannelType type);
    //^ IRQHandler
    virtual bool handle_irq(RegisterState const&) override;
    bool handle_irq_for_dma_transaction();
    void complete_dma_transaction(AsyncDeviceRequest::RequestResult);
    bool handle_irq_for_pio_transaction();
    void complete_pio_transaction(AsyncDeviceRequest::RequestResult);

    void send_ata_pio_command(LBAMode lba_mode, Direction direction) const;
    void ata_read_sectors_with_pio(bool, u16);
    void ata_write_sectors_with_pio(bool, u16);

    void send_ata_dma_command(LBAMode lba_mode, Direction direction) const;
    void ata_read_sectors_with_dma(bool, u16);
    void ata_write_sectors_with_dma(bool, u16);

    void detect_disks();
    StringView channel_type_string() const;

    void try_disambiguate_error();
    bool select_device_and_wait_until_not_busy(DeviceType, size_t milliseconds_timeout);
    bool wait_until_not_busy(size_t milliseconds_timeout);

    void start_request(AsyncBlockDeviceRequest&, bool, u16);

    void clear_pending_interrupts() const;

    void ata_access(Direction, bool, u64, u8, u16);

    bool ata_do_pio_read_sector();
    void ata_do_pio_write_sector();

    PhysicalRegionDescriptor& prdt() { return *reinterpret_cast<PhysicalRegionDescriptor*>(m_prdt_region->vaddr().as_ptr()); }

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
    Mutex m_lock { "IDEChannel"sv };

    bool m_dma_enabled { false };

    IOAddressGroup m_io_group;
    OwnPtr<Memory::Region> m_prdt_region;
    OwnPtr<Memory::Region> m_dma_buffer_region;
    RefPtr<Memory::PhysicalPage> m_prdt_page;
    RefPtr<Memory::PhysicalPage> m_dma_buffer_page;
    NonnullRefPtr<IDEController> m_parent_controller;
};
}
