/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATA/ATADiskDevice.h>
#include <Kernel/Storage/ATA/Definitions.h>
#include <Kernel/Storage/ATA/GenericIDE/Channel.h>
#include <Kernel/Storage/ATA/GenericIDE/Controller.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

#define PATA_PRIMARY_IRQ 14
#define PATA_SECONDARY_IRQ 15

UNMAP_AFTER_INIT NonnullLockRefPtr<IDEChannel> IDEChannel::create(IDEController const& controller, IOAddressGroup io_group, ChannelType type)
{
    auto ata_identify_data_buffer = KBuffer::try_create_with_size("ATA Identify Page"sv, 4096, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow).release_value();
    return adopt_lock_ref(*new IDEChannel(controller, io_group, type, move(ata_identify_data_buffer)));
}

UNMAP_AFTER_INIT NonnullLockRefPtr<IDEChannel> IDEChannel::create(IDEController const& controller, u8 irq, IOAddressGroup io_group, ChannelType type)
{
    auto ata_identify_data_buffer = KBuffer::try_create_with_size("ATA Identify Page"sv, 4096, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow).release_value();
    return adopt_lock_ref(*new IDEChannel(controller, irq, io_group, type, move(ata_identify_data_buffer)));
}

StringView IDEChannel::channel_type_string() const
{
    if (m_channel_type == ChannelType::Primary)
        return "Primary"sv;
    return "Secondary"sv;
}

bool IDEChannel::select_device_and_wait_until_not_busy(DeviceType device_type, size_t milliseconds_timeout)
{
    microseconds_delay(20);
    u8 slave = device_type == DeviceType::Slave;
    m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(0xA0 | (slave << 4)); // First, we need to select the drive itself
    microseconds_delay(20);
    size_t time_elapsed = 0;
    while (m_io_group.control_base().in<u8>() & ATA_SR_BSY && time_elapsed <= milliseconds_timeout) {
        microseconds_delay(1000);
        time_elapsed++;
    }
    return time_elapsed <= milliseconds_timeout;
}

ErrorOr<void> IDEChannel::port_phy_reset()
{
    MutexLocker locker(m_lock);
    SpinlockLocker hard_locker(m_hard_lock);
    // reset the channel
    u8 device_control = m_io_group.control_base().in<u8>();
    // Wait 30 milliseconds
    microseconds_delay(30000);
    m_io_group.control_base().out<u8>(device_control | (1 << 2));
    // Wait 30 milliseconds
    microseconds_delay(30000);
    m_io_group.control_base().out<u8>(device_control);
    // Wait up to 30 seconds before failing
    if (!select_device_and_wait_until_not_busy(DeviceType::Master, 30000)) {
        dbgln("IDEChannel: reset failed, busy flag on master stuck");
        return Error::from_errno(EBUSY);
    }
    // Wait up to 30 seconds before failing
    if (!select_device_and_wait_until_not_busy(DeviceType::Slave, 30000)) {
        dbgln("IDEChannel: reset failed, busy flag on slave stuck");
        return Error::from_errno(EBUSY);
    }
    return {};
}

#if ARCH(I386) || ARCH(X86_64)
ErrorOr<void> IDEChannel::allocate_resources_for_pci_ide_controller(Badge<PCIIDELegacyModeController>, bool force_pio)
{
    return allocate_resources(force_pio);
}
ErrorOr<void> IDEChannel::allocate_resources_for_isa_ide_controller(Badge<ISAIDEController>)
{
    return allocate_resources(false);
}
#endif

UNMAP_AFTER_INIT ErrorOr<void> IDEChannel::allocate_resources(bool force_pio)
{
    dbgln_if(PATA_DEBUG, "IDEChannel: {} IO base: {}", channel_type_string(), m_io_group.io_base());
    dbgln_if(PATA_DEBUG, "IDEChannel: {} control base: {}", channel_type_string(), m_io_group.control_base());
    if (m_io_group.bus_master_base().has_value())
        dbgln_if(PATA_DEBUG, "IDEChannel: {} bus master base: {}", channel_type_string(), m_io_group.bus_master_base().value());
    else
        dbgln_if(PATA_DEBUG, "IDEChannel: {} bus master base disabled", channel_type_string());

    if (!force_pio) {
        m_dma_enabled = true;
        VERIFY(m_io_group.bus_master_base().has_value());
        // Let's try to set up DMA transfers.

        m_prdt_region = TRY(MM.allocate_dma_buffer_page("IDE PRDT"sv, Memory::Region::Access::ReadWrite, m_prdt_page));
        VERIFY(!m_prdt_page.is_null());
        m_dma_buffer_region = TRY(MM.allocate_dma_buffer_page("IDE DMA region"sv, Memory::Region::Access::ReadWrite, m_dma_buffer_page));
        VERIFY(!m_dma_buffer_page.is_null());

        prdt().end_of_table = 0x8000;

        // clear bus master interrupt status
        m_io_group.bus_master_base().value().offset(2).out<u8>(m_io_group.bus_master_base().value().offset(2).in<u8>() | 4);
    }
    return {};
}

UNMAP_AFTER_INIT IDEChannel::IDEChannel(IDEController const& controller, u8 irq, IOAddressGroup io_group, ChannelType type, NonnullOwnPtr<KBuffer> ata_identify_data_buffer)
    : ATAPort(controller, (type == ChannelType::Primary ? 0 : 1), move(ata_identify_data_buffer))
    , IRQHandler(irq)
    , m_channel_type(type)
    , m_io_group(io_group)
{
}

UNMAP_AFTER_INIT IDEChannel::IDEChannel(IDEController const& controller, IOAddressGroup io_group, ChannelType type, NonnullOwnPtr<KBuffer> ata_identify_data_buffer)
    : ATAPort(controller, (type == ChannelType::Primary ? 0 : 1), move(ata_identify_data_buffer))
    , IRQHandler(type == ChannelType::Primary ? PATA_PRIMARY_IRQ : PATA_SECONDARY_IRQ)
    , m_channel_type(type)
    , m_io_group(io_group)
{
}

UNMAP_AFTER_INIT IDEChannel::~IDEChannel() = default;

bool IDEChannel::handle_irq(RegisterState const&)
{
    auto result = handle_interrupt_after_dma_transaction();
    // FIXME: Propagate errors properly
    VERIFY(!result.is_error());
    return result.release_value();
}

ErrorOr<void> IDEChannel::stop_busmastering()
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_io_group.bus_master_base().has_value());
    m_io_group.bus_master_base().value().out<u8>(0);
    return {};
}
ErrorOr<void> IDEChannel::start_busmastering(TransactionDirection direction)
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_io_group.bus_master_base().has_value());
    m_io_group.bus_master_base().value().out<u8>(direction != TransactionDirection::Write ? 0x9 : 0x1);
    return {};
}
ErrorOr<void> IDEChannel::force_busmastering_status_clean()
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_io_group.bus_master_base().has_value());
    m_io_group.bus_master_base().value().offset(2).out<u8>(m_io_group.bus_master_base().value().offset(2).in<u8>() | 4);
    return {};
}
ErrorOr<u8> IDEChannel::busmastering_status()
{
    VERIFY(m_io_group.bus_master_base().has_value());
    return m_io_group.bus_master_base().value().offset(2).in<u8>();
}
ErrorOr<void> IDEChannel::prepare_transaction_with_busmastering(TransactionDirection direction, PhysicalAddress prdt_buffer)
{
    VERIFY(m_lock.is_locked());
    m_io_group.bus_master_base().value().offset(4).out<u32>(prdt_buffer.get());
    m_io_group.bus_master_base().value().out<u8>(direction != TransactionDirection::Write ? 0x8 : 0);
    // Turn on "Interrupt" and "Error" flag. The error flag should be cleared by hardware.
    m_io_group.bus_master_base().value().offset(2).out<u8>(m_io_group.bus_master_base().value().offset(2).in<u8>() | 0x6);
    return {};
}
ErrorOr<void> IDEChannel::initiate_transaction(TransactionDirection)
{
    VERIFY(m_lock.is_locked());
    return {};
}

ErrorOr<u8> IDEChannel::task_file_status()
{
    VERIFY(m_lock.is_locked());
    return m_io_group.control_base().in<u8>();
}

ErrorOr<u8> IDEChannel::task_file_error()
{
    VERIFY(m_lock.is_locked());
    return m_io_group.io_base().offset(ATA_REG_ERROR).in<u8>();
}

ErrorOr<bool> IDEChannel::detect_presence_on_selected_device()
{
    VERIFY(m_lock.is_locked());
    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(0x55);
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>(0xAA);

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(0xAA);
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>(0x55);

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(0x55);
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>(0xAA);

    auto nsectors_value = m_io_group.io_base().offset(ATA_REG_SECCOUNT0).in<u8>();
    auto lba0 = m_io_group.io_base().offset(ATA_REG_LBA0).in<u8>();

    if (lba0 == 0xAA && nsectors_value == 0x55)
        return true;
    return false;
}

ErrorOr<void> IDEChannel::wait_if_busy_until_timeout(size_t timeout_in_milliseconds)
{
    size_t time_elapsed = 0;
    while (m_io_group.control_base().in<u8>() & ATA_SR_BSY && time_elapsed <= timeout_in_milliseconds) {
        microseconds_delay(1000);
        time_elapsed++;
    }
    if (time_elapsed <= timeout_in_milliseconds)
        return {};
    return Error::from_errno(EBUSY);
}

ErrorOr<void> IDEChannel::force_clear_interrupts()
{
    VERIFY(m_lock.is_locked());
    m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>();
    return {};
}

ErrorOr<void> IDEChannel::load_taskfile_into_registers(ATAPort::TaskFile const& task_file, LBAMode lba_mode, size_t completion_timeout_in_milliseconds)
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_hard_lock.is_locked());
    u8 head = 0;
    if (lba_mode == LBAMode::FortyEightBit) {
        head = 0;
    } else if (lba_mode == LBAMode::TwentyEightBit) {
        head = (task_file.lba_high[0] & 0x0F);
    }

    // Note: Preserve the selected drive, always use LBA addressing
    auto driver_register = ((m_io_group.io_base().offset(ATA_REG_HDDEVSEL).in<u8>() & (1 << 4)) | (head | (1 << 5) | (1 << 6)));
    m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(driver_register);
    microseconds_delay(50);

    if (lba_mode == LBAMode::FortyEightBit) {
        m_io_group.io_base().offset(ATA_REG_SECCOUNT1).out<u8>((task_file.count >> 8) & 0xFF);
        m_io_group.io_base().offset(ATA_REG_LBA3).out<u8>(task_file.lba_high[0]);
        m_io_group.io_base().offset(ATA_REG_LBA4).out<u8>(task_file.lba_high[1]);
        m_io_group.io_base().offset(ATA_REG_LBA5).out<u8>(task_file.lba_high[2]);
    }

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(task_file.count & 0xFF);
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>(task_file.lba_low[0]);
    m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>(task_file.lba_low[1]);
    m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>(task_file.lba_low[2]);

    // FIXME: Set a timeout here?
    size_t time_elapsed = 0;
    for (;;) {
        if (time_elapsed > completion_timeout_in_milliseconds)
            return Error::from_errno(EBUSY);
        // FIXME: Use task_file_status method
        auto status = m_io_group.control_base().in<u8>();
        if (!(status & ATA_SR_BSY) && (status & ATA_SR_DRDY))
            break;
        microseconds_delay(1000);
        time_elapsed++;
    }
    m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(task_file.command);
    return {};
}

ErrorOr<void> IDEChannel::device_select(size_t device_index)
{
    VERIFY(m_lock.is_locked());
    if (device_index > 1)
        return Error::from_errno(EINVAL);
    microseconds_delay(20);
    m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(0xA0 | ((device_index) << 4));
    microseconds_delay(20);
    return {};
}

ErrorOr<void> IDEChannel::enable_interrupts()
{
    VERIFY(m_lock.is_locked());
    m_io_group.control_base().out<u8>(0);
    m_interrupts_enabled = true;
    return {};
}
ErrorOr<void> IDEChannel::disable_interrupts()
{
    VERIFY(m_lock.is_locked());
    m_io_group.control_base().out<u8>(1 << 1);
    m_interrupts_enabled = false;
    return {};
}

ErrorOr<void> IDEChannel::read_pio_data_to_buffer(UserOrKernelBuffer& buffer, size_t block_offset, size_t words_count)
{
    VERIFY(m_lock.is_locked());
    VERIFY(words_count == 256);
    for (u32 i = 0; i < 256; ++i) {
        u16 data = m_io_group.io_base().offset(ATA_REG_DATA).in<u16>();
        // FIXME: Don't assume 512 bytes sector
        TRY(buffer.write(&data, block_offset * 512 + (i * 2), 2));
    }
    return {};
}
ErrorOr<void> IDEChannel::write_pio_data_from_buffer(UserOrKernelBuffer const& buffer, size_t block_offset, size_t words_count)
{
    VERIFY(m_lock.is_locked());
    VERIFY(words_count == 256);
    for (u32 i = 0; i < 256; ++i) {
        u16 buf;
        // FIXME: Don't assume 512 bytes sector
        TRY(buffer.read(&buf, block_offset * 512 + (i * 2), 2));
        IO::out16(m_io_group.io_base().offset(ATA_REG_DATA).get(), buf);
    }
    return {};
}
}
