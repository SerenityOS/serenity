/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Storage/ATA/ATADiskDevice.h>
#include <Kernel/Storage/ATA/ATAPort.h>
#include <Kernel/Storage/ATA/Definitions.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

class ATAPortInterruptDisabler {
public:
    ATAPortInterruptDisabler(ATAPort& port)
        : m_port(port)
    {
        (void)port.disable_interrupts();
    }

    ~ATAPortInterruptDisabler()
    {
        (void)m_port->enable_interrupts();
    };

private:
    LockRefPtr<ATAPort> m_port;
};

class ATAPortInterruptCleaner {
public:
    ATAPortInterruptCleaner(ATAPort& port)
        : m_port(port)
    {
    }

    ~ATAPortInterruptCleaner()
    {
        (void)m_port->force_clear_interrupts();
    };

private:
    LockRefPtr<ATAPort> m_port;
};

void ATAPort::fix_name_string_in_identify_device_block()
{
    VERIFY(m_lock.is_locked());
    auto* wbuf = (u16*)m_ata_identify_data_buffer->data();
    auto* bbuf = m_ata_identify_data_buffer->data() + 27 * 2;
    for (size_t word_index = 27; word_index < 47; word_index++) {
        u16 data = wbuf[word_index];
        *(bbuf++) = MSB(data);
        *(bbuf++) = LSB(data);
    }
}

ErrorOr<void> ATAPort::detect_connected_devices()
{
    MutexLocker locker(m_lock);
    for (size_t device_index = 0; device_index < max_possible_devices_connected(); device_index++) {
        TRY(device_select(device_index));
        auto device_presence = TRY(detect_presence_on_selected_device());
        if (!device_presence)
            continue;

        TaskFile identify_taskfile;
        memset(&identify_taskfile, 0, sizeof(TaskFile));
        identify_taskfile.command = ATA_CMD_IDENTIFY;
        auto buffer = UserOrKernelBuffer::for_kernel_buffer(m_ata_identify_data_buffer->data());
        {
            auto result = execute_polled_command(TransactionDirection::Read, LBAMode::None, identify_taskfile, buffer, 0, 256, 100, 100);
            if (result.is_error()) {
                continue;
            }
        }
        ATAIdentifyBlock volatile& identify_block = (ATAIdentifyBlock volatile&)(*m_ata_identify_data_buffer->data());
        u16 capabilities = identify_block.capabilities[0];

        StringView device_name = StringView((char const*)const_cast<u16*>(identify_block.model_number), 40);
        fix_name_string_in_identify_device_block();

        u64 max_addressable_block = identify_block.max_28_bit_addressable_logical_sector;
        dbgln("ATAPort: device found: Name={}, Capacity={}, Capabilities={:#04x}", device_name.trim_whitespace(), max_addressable_block * 512, capabilities);
        // If the drive is so old that it doesn't support LBA, ignore it.
        if (!(capabilities & ATA_CAP_LBA)) {
            dbgln("ATAPort: device found but without LBA support (what kind of dinosaur we see here?)");
            continue;
        }
        // if we support 48-bit LBA, use that value instead.
        if (identify_block.commands_and_feature_sets_supported[1] & (1 << 10))
            max_addressable_block = identify_block.user_addressable_logical_sectors_count;
        // FIXME: Don't assume all drives will have logical sector size of 512 bytes.
        ATADevice::Address address = { m_port_index, static_cast<u8>(device_index) };
        m_ata_devices.append(ATADiskDevice::create(m_parent_ata_controller, address, capabilities, 512, max_addressable_block));
    }
    return {};
}

LockRefPtr<StorageDevice> ATAPort::connected_device(size_t device_index) const
{
    MutexLocker locker(m_lock);
    if (m_ata_devices.size() > device_index)
        return m_ata_devices[device_index];
    return {};
}

ErrorOr<void> ATAPort::start_request(ATADevice const& associated_device, AsyncBlockDeviceRequest& request)
{
    MutexLocker locker(m_lock);
    VERIFY(m_current_request.is_null());
    VERIFY(pio_capable() || dma_capable());

    dbgln_if(ATA_DEBUG, "ATAPort::start_request");

    m_current_request = request;
    m_current_request_block_index = 0;
    m_current_request_flushing_cache = false;

    if (dma_capable()) {
        TRY(prepare_and_initiate_dma_transaction(associated_device));
        return {};
    }
    TRY(prepare_and_initiate_pio_transaction(associated_device));
    return {};
}

void ATAPort::complete_pio_transaction(AsyncDeviceRequest::RequestResult result)
{
    VERIFY(m_current_request);

    // Now schedule reading back the buffer as soon as we leave the irq handler.
    // This is important so that we can safely write the buffer back,
    // which could cause page faults. Note that this may be called immediately
    // before Processor::deferred_call_queue returns!
    auto work_item_creation_result = g_io_work->try_queue([this, result]() {
        dbgln_if(ATA_DEBUG, "ATAPort::complete_pio_transaction result: {}", (int)result);
        MutexLocker locker(m_lock);
        VERIFY(m_current_request);
        auto current_request = m_current_request;
        m_current_request.clear();
        current_request->complete(result);
    });
    if (work_item_creation_result.is_error()) {
        auto current_request = m_current_request;
        m_current_request.clear();
        current_request->complete(AsyncDeviceRequest::OutOfMemory);
    }
}

void ATAPort::complete_dma_transaction(AsyncDeviceRequest::RequestResult result)
{
    // NOTE: this may be called from the interrupt handler!
    VERIFY(m_current_request);
    VERIFY(m_lock.is_locked());

    // Now schedule reading back the buffer as soon as we leave the irq handler.
    // This is important so that we can safely write the buffer back,
    // which could cause page faults. Note that this may be called immediately
    // before Processor::deferred_call_queue returns!
    auto work_item_creation_result = g_io_work->try_queue([this, result]() {
        dbgln_if(ATA_DEBUG, "ATAPort::complete_dma_transaction result: {}", (int)result);
        MutexLocker locker(m_lock);
        if (!m_current_request)
            return;
        auto current_request = m_current_request;
        m_current_request.clear();

        if (result == AsyncDeviceRequest::Success) {
            {
                auto result = force_busmastering_status_clean();
                if (result.is_error()) {
                    locker.unlock();
                    current_request->complete(AsyncDeviceRequest::Failure);
                    return;
                }
            }

            if (current_request->request_type() == AsyncBlockDeviceRequest::Read) {
                if (auto result = current_request->write_to_buffer(current_request->buffer(), m_dma_buffer_region->vaddr().as_ptr(), 512 * current_request->block_count()); result.is_error()) {
                    locker.unlock();
                    current_request->complete(AsyncDeviceRequest::MemoryFault);
                    return;
                }
            }
        }
        locker.unlock();
        current_request->complete(result);
    });
    if (work_item_creation_result.is_error()) {
        auto current_request = m_current_request;
        m_current_request.clear();
        current_request->complete(AsyncDeviceRequest::OutOfMemory);
    }
}

static void print_ata_status(u8 status)
{
    dbgln("ATAPort: print_status: DRQ={} BSY={}, DRDY={}, DSC={}, DF={}, CORR={}, IDX={}, ERR={}",
        (status & ATA_SR_DRQ) != 0,
        (status & ATA_SR_BSY) != 0,
        (status & ATA_SR_DRDY) != 0,
        (status & ATA_SR_DSC) != 0,
        (status & ATA_SR_DF) != 0,
        (status & ATA_SR_CORR) != 0,
        (status & ATA_SR_IDX) != 0,
        (status & ATA_SR_ERR) != 0);
}

static void try_disambiguate_ata_error(u8 error)
{
    dbgln("ATAPort: Error cause:");

    switch (error) {
    case ATA_ER_BBK:
        dbgln("ATAPort: - Bad block");
        break;
    case ATA_ER_UNC:
        dbgln("ATAPort: - Uncorrectable data");
        break;
    case ATA_ER_MC:
        dbgln("ATAPort: - Media changed");
        break;
    case ATA_ER_IDNF:
        dbgln("ATAPort: - ID mark not found");
        break;
    case ATA_ER_MCR:
        dbgln("ATAPort: - Media change request");
        break;
    case ATA_ER_ABRT:
        dbgln("ATAPort: - Command aborted");
        break;
    case ATA_ER_TK0NF:
        dbgln("ATAPort: - Track 0 not found");
        break;
    case ATA_ER_AMNF:
        dbgln("ATAPort: - No address mark");
        break;
    default:
        dbgln("ATAPort: - No one knows");
        break;
    }
}

ErrorOr<bool> ATAPort::handle_interrupt_after_dma_transaction()
{
    if (!dma_capable())
        return false;
    u8 bstatus = TRY(busmastering_status());
    if (!(bstatus & 0x4)) {
        // interrupt not from this device, ignore
        dbgln_if(ATA_DEBUG, "ATAPort: ignore interrupt");
        return false;
    }
    auto work_item_creation_result = g_ata_work->try_queue([this]() -> void {
        MutexLocker locker(m_lock);
        u8 status = task_file_status().release_value();

        m_entropy_source.add_random_event(status);
        // clear bus master interrupt status
        {
            auto result = force_busmastering_status_clean();
            if (result.is_error()) {
                complete_dma_transaction(AsyncDeviceRequest::Failure);
                return;
            }
        }

        SpinlockLocker lock(m_hard_lock);
        dbgln_if(ATA_DEBUG, "ATAPort: interrupt: DRQ={}, BSY={}, DRDY={}",
            (status & ATA_SR_DRQ) != 0,
            (status & ATA_SR_BSY) != 0,
            (status & ATA_SR_DRDY) != 0);

        if (!m_current_request) {
            dbgln("ATAPort: IRQ but no pending request!");
            return;
        }

        if (status & ATA_SR_ERR) {
            print_ata_status(status);
            auto device_error = task_file_error().release_value();
            dbgln("ATAPort: Error {:#02x}!", (u8)device_error);
            try_disambiguate_ata_error(device_error);
            complete_dma_transaction(AsyncDeviceRequest::Failure);
            return;
        }
        complete_dma_transaction(AsyncDeviceRequest::Success);
        return;
    });
    if (work_item_creation_result.is_error()) {
        auto current_request = m_current_request;
        m_current_request.clear();
        current_request->complete(AsyncDeviceRequest::OutOfMemory);
        return Error::from_errno(ENOMEM);
    }
    return true;
}

ErrorOr<void> ATAPort::prepare_and_initiate_dma_transaction(ATADevice const& associated_device)
{
    VERIFY(m_lock.is_locked());
    VERIFY(!m_current_request.is_null());
    VERIFY(m_current_request->block_count() <= 256);

    // Note: We might be called here from an interrupt handler (like the page fault handler), so queue a read afterwards.
    auto work_item_creation_result = g_ata_work->try_queue([this, &associated_device]() -> void {
        MutexLocker locker(m_lock);
        dbgln_if(ATA_DEBUG, "ATAPort::prepare_and_initiate_dma_transaction ({} x {})", m_current_request->block_index(), m_current_request->block_count());

        VERIFY(!m_current_request.is_null());
        VERIFY(m_current_request->block_count() <= 256);
        {
            auto result = device_select(associated_device.ata_address().subport);
            if (result.is_error()) {
                complete_dma_transaction(AsyncDeviceRequest::Failure);
                return;
            }
        }

        if (m_current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Write) {
            if (auto result = m_current_request->read_from_buffer(m_current_request->buffer(), m_dma_buffer_region->vaddr().as_ptr(), 512 * m_current_request->block_count()); result.is_error()) {
                complete_dma_transaction(AsyncDeviceRequest::MemoryFault);
                return;
            }
        }

        prdt().offset = m_dma_buffer_page->paddr().get();
        prdt().size = 512 * m_current_request->block_count();

        VERIFY(prdt().size <= PAGE_SIZE);

        SpinlockLocker hard_lock_locker(m_hard_lock);

        {
            auto result = stop_busmastering();
            if (result.is_error()) {
                complete_dma_transaction(AsyncDeviceRequest::Failure);
                return;
            }
        }

        if (m_current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Write) {
            auto result = prepare_transaction_with_busmastering(TransactionDirection::Write, m_prdt_page->paddr());
            if (result.is_error()) {
                complete_dma_transaction(AsyncDeviceRequest::Failure);
                return;
            }
        } else {
            auto result = prepare_transaction_with_busmastering(TransactionDirection::Read, m_prdt_page->paddr());
            if (result.is_error()) {
                complete_dma_transaction(AsyncDeviceRequest::Failure);
                return;
            }
        }

        TaskFile taskfile;
        LBAMode lba_mode = LBAMode::TwentyEightBit;
        auto lba = m_current_request->block_index();
        if ((lba + m_current_request->block_count()) >= 0x10000000) {
            lba_mode = LBAMode::FortyEightBit;
        }
        memset(&taskfile, 0, sizeof(TaskFile));
        taskfile.lba_low[0] = (lba & 0x000000FF) >> 0;
        taskfile.lba_low[1] = (lba & 0x0000FF00) >> 8;
        taskfile.lba_low[2] = (lba & 0x00FF0000) >> 16;
        taskfile.lba_high[0] = (lba & 0xFF000000) >> 24;
        taskfile.lba_high[1] = (lba & 0xFF00000000ull) >> 32;
        taskfile.lba_high[2] = (lba & 0xFF0000000000ull) >> 40;
        taskfile.count = m_current_request->block_count();
        if (lba_mode == LBAMode::TwentyEightBit)
            taskfile.command = m_current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Write ? ATA_CMD_WRITE_DMA : ATA_CMD_READ_DMA;
        else
            taskfile.command = m_current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Write ? ATA_CMD_WRITE_DMA_EXT : ATA_CMD_READ_DMA_EXT;

        {
            auto result = load_taskfile_into_registers(taskfile, lba_mode, 1000);
            if (result.is_error()) {
                complete_dma_transaction(AsyncDeviceRequest::Failure);
                return;
            }
        }

        if (m_current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Write) {
            auto result = start_busmastering(TransactionDirection::Write);
            if (result.is_error()) {
                complete_dma_transaction(AsyncDeviceRequest::Failure);
                return;
            }
        }

        else {
            auto result = start_busmastering(TransactionDirection::Read);
            if (result.is_error()) {
                complete_dma_transaction(AsyncDeviceRequest::Failure);
                return;
            }
        }
    });
    if (work_item_creation_result.is_error()) {
        auto current_request = m_current_request;
        m_current_request.clear();
        current_request->complete(AsyncDeviceRequest::OutOfMemory);
        return Error::from_errno(ENOMEM);
    }
    return {};
}

ErrorOr<void> ATAPort::prepare_and_initiate_pio_transaction(ATADevice const& associated_device)
{
    VERIFY(m_lock.is_locked());
    VERIFY(!m_current_request.is_null());
    VERIFY(m_current_request->block_count() <= 256);
    dbgln_if(ATA_DEBUG, "ATAPort::prepare_and_initiate_pio_transaction ({} x {})", m_current_request->block_index(), m_current_request->block_count());
    // Note: We might be called here from an interrupt handler (like the page fault handler), so queue a read afterwards.
    auto work_item_creation_result = g_ata_work->try_queue([this, &associated_device]() -> void {
        MutexLocker locker(m_lock);
        {
            auto result = device_select(associated_device.ata_address().subport);
            if (result.is_error()) {
                complete_pio_transaction(AsyncDeviceRequest::Failure);
                return;
            }
        }
        for (size_t block_index = 0; block_index < m_current_request->block_count(); block_index++) {
            TaskFile taskfile;
            LBAMode lba_mode = LBAMode::TwentyEightBit;
            auto lba = m_current_request->block_index() + block_index;
            if (lba >= 0x10000000) {
                lba_mode = LBAMode::FortyEightBit;
            }
            memset(&taskfile, 0, sizeof(TaskFile));
            taskfile.lba_low[0] = (lba & 0x000000FF) >> 0;
            taskfile.lba_low[1] = (lba & 0x0000FF00) >> 8;
            taskfile.lba_low[2] = (lba & 0x00FF0000) >> 16;
            taskfile.lba_high[0] = (lba & 0xFF000000) >> 24;
            taskfile.lba_high[1] = (lba & 0xFF00000000ull) >> 32;
            taskfile.lba_high[2] = (lba & 0xFF0000000000ull) >> 40;
            taskfile.count = 1;
            if (lba_mode == LBAMode::TwentyEightBit)
                taskfile.command = m_current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Write ? ATA_CMD_WRITE_PIO : ATA_CMD_READ_PIO;
            else
                taskfile.command = m_current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Write ? ATA_CMD_WRITE_PIO_EXT : ATA_CMD_READ_PIO_EXT;

            if (m_current_request->request_type() == AsyncBlockDeviceRequest::RequestType::Read) {
                auto result = execute_polled_command(TransactionDirection::Read, lba_mode, taskfile, m_current_request->buffer(), block_index, 256, 100, 100);
                if (result.is_error()) {
                    complete_pio_transaction(AsyncDeviceRequest::Failure);
                    return;
                }

            } else {
                auto result = execute_polled_command(TransactionDirection::Write, lba_mode, taskfile, m_current_request->buffer(), block_index, 256, 100, 100);
                if (result.is_error()) {
                    complete_pio_transaction(AsyncDeviceRequest::Failure);
                    return;
                }
            }
        }
        complete_pio_transaction(AsyncDeviceRequest::Success);
    });
    if (work_item_creation_result.is_error()) {
        auto current_request = m_current_request;
        m_current_request.clear();
        current_request->complete(AsyncDeviceRequest::OutOfMemory);
        return Error::from_errno(ENOMEM);
    }
    return {};
}

ErrorOr<void> ATAPort::execute_polled_command(TransactionDirection direction, LBAMode lba_mode, TaskFile const& taskfile, UserOrKernelBuffer& buffer, size_t block_offset, size_t words_count, size_t preparation_timeout_in_milliseconds, size_t completion_timeout_in_milliseconds)
{
    // Disable interrupts temporarily, just in case we have that enabled,
    // remember the value to re-enable (and clean) later if needed.
    ATAPortInterruptDisabler disabler(*this);
    ATAPortInterruptCleaner cleaner(*this);
    MutexLocker locker(m_lock);
    {
        SpinlockLocker hard_locker(m_hard_lock);

        // Wait for device to be not busy or timeout
        TRY(wait_if_busy_until_timeout(preparation_timeout_in_milliseconds));

        // Send command, wait for result or timeout
        TRY(load_taskfile_into_registers(taskfile, lba_mode, preparation_timeout_in_milliseconds));

        size_t milliseconds_elapsed = 0;
        for (;;) {
            if (milliseconds_elapsed > completion_timeout_in_milliseconds)
                break;
            u8 status = task_file_status().release_value();
            if (status & ATA_SR_ERR) {
                return Error::from_errno(EINVAL);
            }

            if (!(status & ATA_SR_BSY) && (status & ATA_SR_DRQ)) {
                break;
            }

            microseconds_delay(1000);
            milliseconds_elapsed++;
        }
        if (milliseconds_elapsed > completion_timeout_in_milliseconds) {
            critical_dmesgln("ATAPort: device state unknown. Timeout exceeded.");
            return Error::from_errno(EINVAL);
        }
    }

    VERIFY_INTERRUPTS_ENABLED();
    if (direction == TransactionDirection::Read)
        TRY(read_pio_data_to_buffer(buffer, block_offset, words_count));
    else
        TRY(write_pio_data_from_buffer(buffer, block_offset, words_count));
    return {};
}

}
