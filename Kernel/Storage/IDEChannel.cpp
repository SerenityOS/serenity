/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/ByteBuffer.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/IO.h>
#include <Kernel/Process.h>
#include <Kernel/Storage/ATA.h>
#include <Kernel/Storage/IDEChannel.h>
#include <Kernel/Storage/IDEController.h>
#include <Kernel/Storage/PATADiskDevice.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

#define PATA_PRIMARY_IRQ 14
#define PATA_SECONDARY_IRQ 15

#define PCI_Mass_Storage_Class 0x1
#define PCI_IDE_Controller_Subclass 0x1

UNMAP_AFTER_INIT NonnullOwnPtr<IDEChannel> IDEChannel::create(const IDEController& controller, IOAddressGroup io_group, ChannelType type, bool force_pio)
{
    return make<IDEChannel>(controller, io_group, type, force_pio);
}

RefPtr<StorageDevice> IDEChannel::master_device() const
{
    return m_master;
}

RefPtr<StorageDevice> IDEChannel::slave_device() const
{
    return m_slave;
}

UNMAP_AFTER_INIT IDEChannel::IDEChannel(const IDEController& controller, IOAddressGroup io_group, ChannelType type, bool force_pio)
    : IRQHandler(type == ChannelType::Primary ? PATA_PRIMARY_IRQ : PATA_SECONDARY_IRQ)
    , m_channel_type(type)
    , m_io_group(io_group)
    , m_parent_controller(controller)
{
    disable_irq();

    // FIXME: The device may not be capable of DMA.
    m_dma_enabled.resource() = !force_pio;
    ProcFS::add_sys_bool("ide_dma", m_dma_enabled);

    initialize(force_pio);
    detect_disks();

    // Note: calling to detect_disks could generate an interrupt, clear it if that's the case
    clear_pending_interrupts();
    enable_irq();
}

void IDEChannel::clear_pending_interrupts() const
{
    m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>();
}

UNMAP_AFTER_INIT IDEChannel::~IDEChannel()
{
}

void IDEChannel::start_request(AsyncBlockDeviceRequest& request, bool use_dma, bool is_slave, u16 capabilities)
{
    ScopedSpinLock lock(m_request_lock);

    dbgln_if(PATA_DEBUG, "IDEChannel::start_request");

    m_current_request = &request;
    m_current_request_block_index = 0;
    m_current_request_uses_dma = use_dma;
    m_current_request_flushing_cache = false;

    if (request.request_type() == AsyncBlockDeviceRequest::Read) {
        if (use_dma)
            ata_read_sectors_with_dma(is_slave, capabilities);
        else
            ata_read_sectors(is_slave, capabilities);
    } else {
        if (use_dma)
            ata_write_sectors_with_dma(is_slave, capabilities);
        else
            ata_write_sectors(is_slave, capabilities);
    }
}

void IDEChannel::complete_current_request(AsyncDeviceRequest::RequestResult result)
{
    // NOTE: this may be called from the interrupt handler!
    VERIFY(m_current_request);
    VERIFY(m_request_lock.is_locked());

    // Now schedule reading back the buffer as soon as we leave the irq handler.
    // This is important so that we can safely write the buffer back,
    // which could cause page faults. Note that this may be called immediately
    // before Processor::deferred_call_queue returns!
    g_io_work->queue([this, result]() {
        dbgln_if(PATA_DEBUG, "IDEChannel::complete_current_request result: {}", (int)result);
        ScopedSpinLock lock(m_request_lock);
        VERIFY(m_current_request);
        auto& request = *m_current_request;
        m_current_request = nullptr;

        if (m_current_request_uses_dma) {
            if (result == AsyncDeviceRequest::Success) {
                if (request.request_type() == AsyncBlockDeviceRequest::Read) {
                    if (!request.write_to_buffer(request.buffer(), m_dma_buffer_page->paddr().offset(0xc0000000).as_ptr(), 512 * request.block_count())) {
                        lock.unlock();
                        request.complete(AsyncDeviceRequest::MemoryFault);
                        return;
                    }
                }

                // I read somewhere that this may trigger a cache flush so let's do it.
                m_io_group.bus_master_base().offset(2).out<u8>(m_io_group.bus_master_base().offset(2).in<u8>() | 0x6);
            }
        }

        lock.unlock();
        request.complete(result);
    });
}

UNMAP_AFTER_INIT void IDEChannel::initialize(bool force_pio)
{
    m_parent_controller->enable_pin_based_interrupts();

    dbgln_if(PATA_DEBUG, "IDEChannel: {} IO base: {}", channel_type_string(), m_io_group.io_base());
    dbgln_if(PATA_DEBUG, "IDEChannel: {} control base: {}", channel_type_string(), m_io_group.control_base());
    dbgln_if(PATA_DEBUG, "IDEChannel: {} bus master base: {}", channel_type_string(), m_io_group.bus_master_base());

    if (force_pio) {
        dbgln("IDEChannel: Requested to force PIO mode; not setting up DMA");
        return;
    }

    // Let's try to set up DMA transfers.
    PCI::enable_bus_mastering(m_parent_controller->pci_address());
    m_prdt_page = MM.allocate_supervisor_physical_page();
    prdt().end_of_table = 0x8000;
    m_dma_buffer_page = MM.allocate_supervisor_physical_page();
}

static void print_ide_status(u8 status)
{
    dbgln("IDEChannel: print_ide_status: DRQ={} BSY={}, DRDY={}, DSC={}, DF={}, CORR={}, IDX={}, ERR={}",
        (status & ATA_SR_DRQ) != 0,
        (status & ATA_SR_BSY) != 0,
        (status & ATA_SR_DRDY) != 0,
        (status & ATA_SR_DSC) != 0,
        (status & ATA_SR_DF) != 0,
        (status & ATA_SR_CORR) != 0,
        (status & ATA_SR_IDX) != 0,
        (status & ATA_SR_ERR) != 0);
}

void IDEChannel::try_disambiguate_error()
{
    dbgln("IDEChannel: Error cause:");

    switch (m_device_error) {
    case ATA_ER_BBK:
        dbgln("IDEChannel: - Bad block");
        break;
    case ATA_ER_UNC:
        dbgln("IDEChannel: - Uncorrectable data");
        break;
    case ATA_ER_MC:
        dbgln("IDEChannel: - Media changed");
        break;
    case ATA_ER_IDNF:
        dbgln("IDEChannel: - ID mark not found");
        break;
    case ATA_ER_MCR:
        dbgln("IDEChannel: - Media change request");
        break;
    case ATA_ER_ABRT:
        dbgln("IDEChannel: - Command aborted");
        break;
    case ATA_ER_TK0NF:
        dbgln("IDEChannel: - Track 0 not found");
        break;
    case ATA_ER_AMNF:
        dbgln("IDEChannel: - No address mark");
        break;
    default:
        dbgln("IDEChannel: - No one knows");
        break;
    }
}

void IDEChannel::handle_irq(const RegisterState&)
{
    u8 status = m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>();

    m_entropy_source.add_random_event(status);

    u8 bstatus = m_io_group.bus_master_base().offset(2).in<u8>();
    if (!(bstatus & 0x4)) {
        // interrupt not from this device, ignore
        dbgln_if(PATA_DEBUG, "IDEChannel: ignore interrupt");
        return;
    }

    ScopedSpinLock lock(m_request_lock);
    dbgln_if(PATA_DEBUG, "IDEChannel: interrupt: DRQ={}, BSY={}, DRDY={}",
        (status & ATA_SR_DRQ) != 0,
        (status & ATA_SR_BSY) != 0,
        (status & ATA_SR_DRDY) != 0);

    if (!m_current_request) {
        dbgln("IDEChannel: IRQ but no pending request!");
        return;
    }

    if (status & ATA_SR_ERR) {
        print_ide_status(status);
        m_device_error = m_io_group.io_base().offset(ATA_REG_ERROR).in<u8>();
        dbgln("IDEChannel: Error {:#02x}!", (u8)m_device_error);
        try_disambiguate_error();
        complete_current_request(AsyncDeviceRequest::Failure);
        return;
    }
    m_device_error = 0;
    if (m_current_request_uses_dma) {
        complete_current_request(AsyncDeviceRequest::Success);
        return;
    }

    // Now schedule reading/writing the buffer as soon as we leave the irq handler.
    // This is important so that we can safely access the buffers, which could
    // trigger page faults
    Processor::deferred_call_queue([this]() {
        ScopedSpinLock lock(m_request_lock);
        if (m_current_request->request_type() == AsyncBlockDeviceRequest::Read) {
            dbgln_if(PATA_DEBUG, "IDEChannel: Read block {}/{}", m_current_request_block_index, m_current_request->block_count());
            if (ata_do_read_sector()) {
                if (++m_current_request_block_index >= m_current_request->block_count()) {
                    complete_current_request(AsyncDeviceRequest::Success);
                    return;
                }
                // Wait for the next block
                enable_irq();
            }
        } else {
            if (!m_current_request_flushing_cache) {
                dbgln_if(PATA_DEBUG, "IDEChannel: Wrote block {}/{}", m_current_request_block_index, m_current_request->block_count());
                if (++m_current_request_block_index >= m_current_request->block_count()) {
                    // We read the last block, flush cache
                    VERIFY(!m_current_request_flushing_cache);
                    m_current_request_flushing_cache = true;
                    m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(ATA_CMD_CACHE_FLUSH);
                } else {
                    // Read next block
                    ata_do_write_sector();
                }
            } else {
                complete_current_request(AsyncDeviceRequest::Success);
            }
        }
    });
}

static void io_delay()
{
    for (int i = 0; i < 4; ++i)
        IO::in8(0x3f6);
}

void IDEChannel::wait_until_not_busy()
{
    while (m_io_group.control_base().in<u8>() & ATA_SR_BSY)
        ;
}

String IDEChannel::channel_type_string() const
{
    if (m_channel_type == ChannelType::Primary)
        return "Primary";

    return "Secondary";
}

UNMAP_AFTER_INIT void IDEChannel::detect_disks()
{
    auto channel_string = [](u8 i) -> const char* {
        if (i == 0)
            return "master";

        return "slave";
    };

    // There are only two possible disks connected to a channel
    for (auto i = 0; i < 2; i++) {
        m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(0xA0 | (i << 4)); // First, we need to select the drive itself

        m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(ATA_CMD_IDENTIFY); // Send the ATA_IDENTIFY command

        // Wait for the BSY flag to be reset
        while (m_io_group.control_base().in<u8>() & ATA_SR_BSY)
            ;

        if (m_io_group.control_base().in<u8>() == 0x00) {
            dbgln_if(PATA_DEBUG, "IDEChannel: No {} {} disk detected!", channel_type_string().to_lowercase(), channel_string(i));
            continue;
        }

        bool check_for_atapi = false;
        PATADiskDevice::InterfaceType interface_type = PATADiskDevice::InterfaceType::ATA;

        for (;;) {
            u8 status = m_io_group.control_base().in<u8>();
            if (status & ATA_SR_ERR) {
                dbgln_if(PATA_DEBUG, "IDEChannel: {} {} device is not ATA. Will check for ATAPI.", channel_type_string(), channel_string(i));
                check_for_atapi = true;
                break;
            }

            if (!(status & ATA_SR_BSY) && (status & ATA_SR_DRQ)) {
                dbgln_if(PATA_DEBUG, "IDEChannel: {} {} device appears to be ATA.", channel_type_string(), channel_string(i));
                interface_type = PATADiskDevice::InterfaceType::ATA;
                break;
            }
        }

        if (check_for_atapi) {
            u8 cl = m_io_group.io_base().offset(ATA_REG_LBA1).in<u8>();
            u8 ch = m_io_group.io_base().offset(ATA_REG_LBA2).in<u8>();

            if ((cl == 0x14 && ch == 0xEB) || (cl == 0x69 && ch == 0x96)) {
                interface_type = PATADiskDevice::InterfaceType::ATAPI;
                dbgln("IDEChannel: {} {} device appears to be ATAPI. We're going to ignore it for now as we don't support it.", channel_type_string(), channel_string(i));
                continue;
            } else {
                dbgln("IDEChannel: {} {} device doesn't appear to be ATA or ATAPI. Ignoring it.", channel_type_string(), channel_string(i));
                continue;
            }
        }

        ByteBuffer wbuf = ByteBuffer::create_uninitialized(512);
        ByteBuffer bbuf = ByteBuffer::create_uninitialized(512);
        u8* b = bbuf.data();
        u16* w = (u16*)wbuf.data();
        const u16* wbufbase = (u16*)wbuf.data();

        for (u32 i = 0; i < 256; ++i) {
            u16 data = m_io_group.io_base().offset(ATA_REG_DATA).in<u16>();
            *(w++) = data;
            *(b++) = MSB(data);
            *(b++) = LSB(data);
        }

        // "Unpad" the device name string.
        for (u32 i = 93; i > 54 && bbuf[i] == ' '; --i)
            bbuf[i] = 0;

        u16 cyls = wbufbase[ATA_IDENT_CYLINDERS / sizeof(u16)];
        u16 heads = wbufbase[ATA_IDENT_HEADS / sizeof(u16)];
        u16 spt = wbufbase[ATA_IDENT_SECTORS / sizeof(u16)];
        u16 capabilities = wbufbase[ATA_IDENT_CAPABILITIES / sizeof(u16)];
        if (cyls == 0 || heads == 0 || spt == 0)
            continue;
        u64 max_addressable_block = cyls * heads * spt;
        if (capabilities & ATA_CAP_LBA)
            max_addressable_block = (wbufbase[(ATA_IDENT_MAX_LBA + 2) / sizeof(u16)] << 16) | wbufbase[ATA_IDENT_MAX_LBA / sizeof(u16)];
        dbgln("IDEChannel: {} {} {} device found: Name={}, Capacity={}, C/H/Spt={}/{}/{}, Capabilities=0x{:04x}", channel_type_string(), channel_string(i), interface_type == PATADiskDevice::InterfaceType::ATA ? "ATA" : "ATAPI", ((char*)bbuf.data() + 54), max_addressable_block * 512, cyls, heads, spt, capabilities);

        if (i == 0) {
            m_master = PATADiskDevice::create(m_parent_controller, *this, PATADiskDevice::DriveType::Master, interface_type, capabilities, max_addressable_block);
        } else {
            m_slave = PATADiskDevice::create(m_parent_controller, *this, PATADiskDevice::DriveType::Slave, interface_type, capabilities, max_addressable_block);
        }
    }
}

void IDEChannel::ata_access(Direction direction, bool slave_request, u64 lba, u8 block_count, u16 capabilities, bool use_dma)
{
    LBAMode lba_mode;
    u8 head = 0;
    u8 sector = 0;
    u16 cylinder = 0;

    if (lba >= 0x10000000) {
        VERIFY(capabilities & ATA_CAP_LBA);
        lba_mode = LBAMode::FortyEightBit;
        head = 0;
    } else if (capabilities & ATA_CAP_LBA) {
        lba_mode = LBAMode::TwentyEightBit;
        head = (lba & 0xF000000) >> 24;
    } else {
        lba_mode = LBAMode::None;
        sector = (lba % 63) + 1;
        cylinder = (lba + 1 - sector) / (16 * 63);
        head = (lba + 1 - sector) % (16 * 63) / (63);
    }

    wait_until_not_busy();

    if (lba_mode == LBAMode::None)
        m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(0xA0 | (static_cast<u8>(slave_request) << 4) | head);
    else
        m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(0xE0 | (static_cast<u8>(slave_request) << 4) | head);

    if (lba_mode == LBAMode::FortyEightBit) {
        m_io_group.io_base().offset(ATA_REG_SECCOUNT1).out<u8>(0);
        m_io_group.io_base().offset(ATA_REG_LBA3).out<u8>((lba & 0xFF000000) >> 24);
        m_io_group.io_base().offset(ATA_REG_LBA4).out<u8>((lba & 0xFF00000000ull) >> 32);
        m_io_group.io_base().offset(ATA_REG_LBA5).out<u8>((lba & 0xFF0000000000ull) >> 40);
    }

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(block_count);
    if (lba_mode == LBAMode::FortyEightBit || lba_mode == LBAMode::TwentyEightBit) {
        m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>((lba & 0x000000FF) >> 0);
        m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>((lba & 0x0000FF00) >> 8);
        m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>((lba & 0x00FF0000) >> 16);
    } else {
        m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>(sector);
        m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>((cylinder >> 0) & 0xFF);
        m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>((cylinder >> 8) & 0xFF);
    }

    for (;;) {
        auto status = m_io_group.control_base().in<u8>();
        if (!(status & ATA_SR_BSY) && (status & ATA_SR_DRDY))
            break;
    }

    if (lba_mode != LBAMode::FortyEightBit) {
        if (use_dma)
            m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(direction == Direction::Read ? ATA_CMD_READ_DMA : ATA_CMD_WRITE_DMA);
        else
            m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(direction == Direction::Read ? ATA_CMD_READ_PIO : ATA_CMD_WRITE_PIO);
    } else {
        if (use_dma)
            m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(direction == Direction::Read ? ATA_CMD_READ_DMA_EXT : ATA_CMD_WRITE_DMA_EXT);
        else
            m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(direction == Direction::Read ? ATA_CMD_READ_PIO_EXT : ATA_CMD_WRITE_PIO_EXT);
    }
    enable_irq();
}

void IDEChannel::ata_read_sectors_with_dma(bool slave_request, u16 capabilities)
{
    auto& request = *m_current_request;
    auto lba = request.block_index();
    dbgln_if(PATA_DEBUG, "IDEChannel::ata_read_sectors_with_dma ({} x {})", lba, request.block_count());

    prdt().offset = m_dma_buffer_page->paddr();
    prdt().size = 512 * request.block_count();

    VERIFY(prdt().size <= PAGE_SIZE);

    // Stop bus master
    m_io_group.bus_master_base().out<u8>(0);

    // Write the PRDT location
    m_io_group.bus_master_base().offset(4).out(m_prdt_page->paddr().get());

    // Turn on "Interrupt" and "Error" flag. The error flag should be cleared by hardware.
    m_io_group.bus_master_base().offset(2).out<u8>(m_io_group.bus_master_base().offset(2).in<u8>() | 0x6);

    // Set transfer direction
    m_io_group.bus_master_base().out<u8>(0x8);

    ata_access(Direction::Read, slave_request, lba, request.block_count(), capabilities, true);

    // Start bus master
    m_io_group.bus_master_base().out<u8>(0x9);
}

bool IDEChannel::ata_do_read_sector()
{
    dbgln_if(PATA_DEBUG, "IDEChannel::ata_do_read_sector");
    auto& request = *m_current_request;
    auto out_buffer = request.buffer().offset(m_current_request_block_index * 512);
    ssize_t nwritten = request.write_to_buffer_buffered<512>(out_buffer, 512, [&](u8* buffer, size_t buffer_bytes) {
        for (size_t i = 0; i < buffer_bytes; i += sizeof(u16))
            *(u16*)&buffer[i] = IO::in16(m_io_group.io_base().offset(ATA_REG_DATA).get());
        return (ssize_t)buffer_bytes;
    });
    if (nwritten < 0) {
        // TODO: Do we need to abort the PATA read if this wasn't the last block?
        complete_current_request(AsyncDeviceRequest::MemoryFault);
        return false;
    }
    return true;
}

// FIXME: This doesn't quite work and locks up reading LBA 3.
void IDEChannel::ata_read_sectors(bool slave_request, u16 capabilities)
{
    auto& request = *m_current_request;
    VERIFY(request.block_count() <= 256);
    dbgln_if(PATA_DEBUG, "IDEChannel::ata_read_sectors");

    auto lba = request.block_index();
    dbgln_if(PATA_DEBUG, "IDEChannel: Reading {} sector(s) @ LBA {}", request.block_count(), lba);

    ata_access(Direction::Read, slave_request, lba, request.block_count(), capabilities, false);
}

void IDEChannel::ata_write_sectors_with_dma(bool slave_request, u16 capabilities)
{
    auto& request = *m_current_request;
    auto lba = request.block_index();
    dbgln_if(PATA_DEBUG, "IDEChannel::ata_write_sectors_with_dma ({} x {})", lba, request.block_count());

    prdt().offset = m_dma_buffer_page->paddr();
    prdt().size = 512 * request.block_count();

    if (!request.read_from_buffer(request.buffer(), m_dma_buffer_page->paddr().offset(0xc0000000).as_ptr(), 512 * request.block_count())) {
        complete_current_request(AsyncDeviceRequest::MemoryFault);
        return;
    }

    VERIFY(prdt().size <= PAGE_SIZE);

    // Stop bus master
    m_io_group.bus_master_base().out<u8>(0);

    // Write the PRDT location
    m_io_group.bus_master_base().offset(4).out<u32>(m_prdt_page->paddr().get());

    // Turn on "Interrupt" and "Error" flag. The error flag should be cleared by hardware.
    m_io_group.bus_master_base().offset(2).out<u8>(m_io_group.bus_master_base().offset(2).in<u8>() | 0x6);

    ata_access(Direction::Write, slave_request, lba, request.block_count(), capabilities, true);

    // Start bus master
    m_io_group.bus_master_base().out<u8>(0x1);
}

void IDEChannel::ata_do_write_sector()
{
    auto& request = *m_current_request;

    io_delay();
    while ((m_io_group.control_base().in<u8>() & ATA_SR_BSY) || !(m_io_group.control_base().in<u8>() & ATA_SR_DRQ))
        ;

    u8 status = m_io_group.control_base().in<u8>();
    VERIFY(status & ATA_SR_DRQ);

    auto in_buffer = request.buffer().offset(m_current_request_block_index * 512);
    dbgln_if(PATA_DEBUG, "IDEChannel: Writing 512 bytes (part {}) (status={:#02x})...", m_current_request_block_index, status);
    ssize_t nread = request.read_from_buffer_buffered<512>(in_buffer, 512, [&](const u8* buffer, size_t buffer_bytes) {
        for (size_t i = 0; i < buffer_bytes; i += sizeof(u16))
            IO::out16(m_io_group.io_base().offset(ATA_REG_DATA).get(), *(const u16*)&buffer[i]);
        return (ssize_t)buffer_bytes;
    });
    if (nread < 0)
        complete_current_request(AsyncDeviceRequest::MemoryFault);
}

// FIXME: I'm assuming this doesn't work based on the fact PIO read doesn't work.
void IDEChannel::ata_write_sectors(bool slave_request, u16 capabilities)
{
    auto& request = *m_current_request;

    VERIFY(request.block_count() <= 256);
    auto start_sector = request.block_index();
    u32 count = request.block_count();
    dbgln_if(PATA_DEBUG, "IDEChannel: Writing {} sector(s) @ LBA {}", count, start_sector);

    ata_access(Direction::Write, slave_request, start_sector, request.block_count(), capabilities, false);
    ata_do_write_sector();
}
}
