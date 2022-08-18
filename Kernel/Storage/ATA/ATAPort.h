/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Storage/ATA/ATADevice.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class ATAPort
    : public RefCounted<ATAPort>
    , public Weakable<ATAPort> {

    friend class ATAPortInterruptDisabler;
    friend class ATAPortInterruptCleaner;

public:
    struct TaskFile {
        u8 command;
        u8 lba_low[3];
        u8 device;
        u8 lba_high[3];
        u8 features_high;
        u16 count;
        u8 icc;
        u8 control;
        u32 reserved;
    };

    enum class TransactionDirection : u8 {
        Read,
        Write,
    };

    struct [[gnu::packed]] PhysicalRegionDescriptor {
        u32 offset;
        u16 size { 0 };
        u16 end_of_table { 0 };
    };

    enum class LBAMode : u8 {
        None,
        TwentyEightBit,
        FortyEightBit,
    };

public:
    RefPtr<StorageDevice> connected_device(size_t device_index) const;

    virtual ~ATAPort() = default;

    virtual ErrorOr<void> disable() = 0;
    virtual ErrorOr<void> power_on() = 0;
    ErrorOr<void> detect_connected_devices();
    ErrorOr<bool> handle_interrupt_after_dma_transaction();

    ErrorOr<void> start_request(ATADevice const& associated_device, AsyncBlockDeviceRequest&);

    // Note: Generic (P)ATA IDE "ports" are tied to the IDE channel link (cable), and trying to
    // reset the master port or slave port and vice versa requires to actually reset
    // both at once...
    // This is due to the fact that IDE devices can be connected together (master-slave)
    // with one 80 pin cable which forms one (primary/secondary) "ATA bus".
    // Intel AHCI controllers generally allow individual phy port reset. The caller
    // of this method should know this in advance...
    // Note: ATAPI devices are an exception to this, so even if we initiate a
    // a port reset, there's no guarantee that ATAPI devices will reset anyway,
    // so resetting them requires to actually send the ATA "DEVICE RESET" command.
    virtual ErrorOr<void> port_phy_reset() = 0;

    // Note: Software reset means individual reset to a selected device on the "bus" (port).
    // This means that this will likely work for devices that indicate support for
    // PACKET commands (ATAPI devices) that also support DEVICE RESET. For other devices
    // there's no other method to reset them besides (full) PHY reset.
    // For devices that don't support this feature, just return ENOTSUP.
    virtual ErrorOr<void> soft_reset() { return Error::from_errno(ENOTSUP); }

    ErrorOr<void> execute_polled_command(TransactionDirection direction, LBAMode lba_mode, TaskFile const& taskfile, UserOrKernelBuffer&, size_t block_offset, size_t words_count, size_t preparation_timeout_in_milliseconds, size_t completion_timeout_in_milliseconds);

    virtual bool has_sata_capabilities() { return false; }

    virtual bool pio_capable() const = 0;
    virtual bool dma_capable() const = 0;

    virtual size_t max_possible_devices_connected() const = 0;

private:
    ErrorOr<void> prepare_and_initiate_dma_transaction(ATADevice const& associated_device);
    ErrorOr<void> prepare_and_initiate_pio_transaction(ATADevice const& associated_device);

    void complete_dma_transaction(AsyncDeviceRequest::RequestResult result);
    void complete_pio_transaction(AsyncDeviceRequest::RequestResult result);

    void fix_name_string_in_identify_device_block();

protected:
    virtual ErrorOr<u8> task_file_status() = 0;
    virtual ErrorOr<u8> task_file_error() = 0;

    virtual ErrorOr<void> wait_if_busy_until_timeout(size_t timeout_in_milliseconds) = 0;

    virtual ErrorOr<void> device_select(size_t device_index) = 0;
    virtual ErrorOr<bool> detect_presence_on_selected_device() = 0;

    virtual ErrorOr<void> enable_interrupts() = 0;
    virtual ErrorOr<void> disable_interrupts() = 0;

    virtual ErrorOr<void> stop_busmastering() = 0;
    virtual ErrorOr<void> start_busmastering(TransactionDirection) = 0;
    virtual ErrorOr<void> force_busmastering_status_clean() = 0;
    virtual ErrorOr<u8> busmastering_status() = 0;
    virtual ErrorOr<void> prepare_transaction_with_busmastering(TransactionDirection, PhysicalAddress prdt_buffer) = 0;
    virtual ErrorOr<void> initiate_transaction(TransactionDirection) = 0;

    virtual ErrorOr<void> force_clear_interrupts() = 0;

    // Note: This method assume we already selected the correct device!
    virtual ErrorOr<void> load_taskfile_into_registers(TaskFile const&, LBAMode lba_mode, size_t completion_timeout_in_milliseconds) = 0;

    virtual ErrorOr<void> read_pio_data_to_buffer(UserOrKernelBuffer&, size_t block_offset, size_t words_count) = 0;
    virtual ErrorOr<void> write_pio_data_from_buffer(UserOrKernelBuffer const&, size_t block_offset, size_t words_count) = 0;

    PhysicalRegionDescriptor& prdt() { return *reinterpret_cast<PhysicalRegionDescriptor*>(m_prdt_region->vaddr().as_ptr()); }

    ATAPort(ATAController const& parent_controller, u8 port_index, NonnullOwnPtr<KBuffer> ata_identify_data_buffer)
        : m_port_index(port_index)
        , m_ata_identify_data_buffer(move(ata_identify_data_buffer))
        , m_parent_ata_controller(parent_controller)
    {
    }

    mutable Mutex m_lock;
    Spinlock m_hard_lock { LockRank::None };

    EntropySource m_entropy_source;

    RefPtr<AsyncBlockDeviceRequest> m_current_request;
    u64 m_current_request_block_index { 0 };
    bool m_current_request_flushing_cache { false };

    OwnPtr<Memory::Region> m_prdt_region;
    OwnPtr<Memory::Region> m_dma_buffer_region;
    RefPtr<Memory::PhysicalPage> m_prdt_page;
    RefPtr<Memory::PhysicalPage> m_dma_buffer_page;

    const u8 m_port_index;
    NonnullRefPtrVector<ATADevice> m_ata_devices;
    NonnullOwnPtr<KBuffer> m_ata_identify_data_buffer;
    NonnullRefPtr<ATAController> m_parent_ata_controller;
};
}
