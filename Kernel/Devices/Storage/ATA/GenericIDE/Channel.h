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

#include <AK/Error.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Storage/ATA/ATADevice.h>
#include <Kernel/Devices/Storage/ATA/ATAPort.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class IDEController;
#if ARCH(X86_64)
class PCIIDELegacyModeController;
class ISAIDEController;
#endif
class IDEChannel
    : public ATAPort
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

    struct IOWindowGroup {
        IOWindowGroup(NonnullOwnPtr<IOWindow> io_window, NonnullOwnPtr<IOWindow> control_window, NonnullOwnPtr<IOWindow> m_bus_master_window)
            : m_io_window(move(io_window))
            , m_control_window(move(control_window))
            , m_bus_master_window(move(m_bus_master_window))
        {
        }

        IOWindowGroup(NonnullOwnPtr<IOWindow> io_window, NonnullOwnPtr<IOWindow> control_window)
            : m_io_window(move(io_window))
            , m_control_window(move(control_window))
        {
        }

        // Disable default implementations that would use surprising integer promotion.
        bool operator==(IOWindowGroup const&) const = delete;
        bool operator<=(IOWindowGroup const&) const = delete;
        bool operator>=(IOWindowGroup const&) const = delete;
        bool operator<(IOWindowGroup const&) const = delete;
        bool operator>(IOWindowGroup const&) const = delete;

        IOWindow& io_window() const { return *m_io_window; }
        IOWindow& control_window() const { return *m_control_window; }
        IOWindow* bus_master_window() const { return m_bus_master_window.ptr(); }

    private:
        mutable NonnullOwnPtr<IOWindow> m_io_window;
        mutable NonnullOwnPtr<IOWindow> m_control_window;
        mutable OwnPtr<IOWindow> m_bus_master_window;
    };

public:
    static ErrorOr<NonnullRefPtr<IDEChannel>> create(IDEController const&, IOWindowGroup, ChannelType type);
    static ErrorOr<NonnullRefPtr<IDEChannel>> create(IDEController const&, u8 irq, IOWindowGroup, ChannelType type);

    virtual ~IDEChannel() override;

    virtual StringView purpose() const override { return "PATA Channel"sv; }

#if ARCH(X86_64)
    ErrorOr<void> allocate_resources_for_pci_ide_controller(Badge<PCIIDELegacyModeController>, bool force_pio);
    ErrorOr<void> allocate_resources_for_isa_ide_controller(Badge<ISAIDEController>);
#endif

private:
    static constexpr size_t m_logical_sector_size = 512;
    ErrorOr<void> allocate_resources(bool force_pio);
    StringView channel_type_string() const;

    virtual ErrorOr<void> disable() override { TODO(); }
    virtual ErrorOr<void> power_on() override { TODO(); }

    virtual ErrorOr<void> port_phy_reset() override;
    bool select_device_and_wait_until_not_busy(DeviceType, size_t milliseconds_timeout);

    virtual bool pio_capable() const override { return true; }
    virtual bool dma_capable() const override { return m_dma_enabled; }

    virtual size_t max_possible_devices_connected() const override { return 2; }

    virtual ErrorOr<void> stop_busmastering() override;
    virtual ErrorOr<void> start_busmastering(TransactionDirection) override;
    virtual ErrorOr<void> force_busmastering_status_clean() override;
    virtual ErrorOr<u8> busmastering_status() override;
    virtual ErrorOr<void> prepare_transaction_with_busmastering(TransactionDirection, PhysicalAddress prdt_buffer) override;
    virtual ErrorOr<void> initiate_transaction(TransactionDirection) override;

    virtual ErrorOr<u8> task_file_status() override;
    virtual ErrorOr<u8> task_file_error() override;

    virtual ErrorOr<void> wait_if_busy_until_timeout(size_t timeout_in_milliseconds) override;

    virtual ErrorOr<void> device_select(size_t device_index) override;
    virtual ErrorOr<bool> detect_presence_on_selected_device() override;

    virtual ErrorOr<void> enable_interrupts() override;
    virtual ErrorOr<void> disable_interrupts() override;

    virtual ErrorOr<void> force_clear_interrupts() override;
    virtual ErrorOr<void> load_taskfile_into_registers(TaskFile const&, LBAMode lba_mode, size_t completion_timeout_in_milliseconds) override;

    virtual ErrorOr<void> read_pio_data_to_buffer(UserOrKernelBuffer&, size_t block_offset, size_t words_count) override;
    virtual ErrorOr<void> write_pio_data_from_buffer(UserOrKernelBuffer const&, size_t block_offset, size_t words_count) override;

    IDEChannel(IDEController const&, IOWindowGroup, ChannelType type, NonnullOwnPtr<KBuffer> ata_identify_data_buffer);
    IDEChannel(IDEController const&, u8 irq, IOWindowGroup, ChannelType type, NonnullOwnPtr<KBuffer> ata_identify_data_buffer);
    //^ IRQHandler
    virtual bool handle_irq(RegisterState const&) override;

    // Data members
    ChannelType m_channel_type { ChannelType::Primary };

    bool m_dma_enabled { false };
    bool m_interrupts_enabled { true };

    IOWindowGroup m_io_window_group;
};
}
