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

#include <AK/ByteBuffer.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/IO.h>
#include <Kernel/Process.h>
#include <Kernel/Storage/IDEChannel.h>
#include <Kernel/Storage/IDEController.h>
#include <Kernel/Storage/PATADiskDevice.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

#define PATA_PRIMARY_IRQ 14
#define PATA_SECONDARY_IRQ 15

#define ATA_SR_BSY 0x80
#define ATA_SR_DRDY 0x40
#define ATA_SR_DF 0x20
#define ATA_SR_DSC 0x10
#define ATA_SR_DRQ 0x08
#define ATA_SR_CORR 0x04
#define ATA_SR_IDX 0x02
#define ATA_SR_ERR 0x01

#define ATA_ER_BBK 0x80
#define ATA_ER_UNC 0x40
#define ATA_ER_MC 0x20
#define ATA_ER_IDNF 0x10
#define ATA_ER_MCR 0x08
#define ATA_ER_ABRT 0x04
#define ATA_ER_TK0NF 0x02
#define ATA_ER_AMNF 0x01

#define ATA_CMD_READ_PIO 0x20
#define ATA_CMD_READ_PIO_EXT 0x24
#define ATA_CMD_READ_DMA 0xC8
#define ATA_CMD_READ_DMA_EXT 0x25
#define ATA_CMD_WRITE_PIO 0x30
#define ATA_CMD_WRITE_PIO_EXT 0x34
#define ATA_CMD_WRITE_DMA 0xCA
#define ATA_CMD_WRITE_DMA_EXT 0x35
#define ATA_CMD_CACHE_FLUSH 0xE7
#define ATA_CMD_CACHE_FLUSH_EXT 0xEA
#define ATA_CMD_PACKET 0xA0
#define ATA_CMD_IDENTIFY_PACKET 0xA1
#define ATA_CMD_IDENTIFY 0xEC

#define ATAPI_CMD_READ 0xA8
#define ATAPI_CMD_EJECT 0x1B

#define ATA_IDENT_DEVICETYPE 0
#define ATA_IDENT_CYLINDERS 2
#define ATA_IDENT_HEADS 6
#define ATA_IDENT_SECTORS 12
#define ATA_IDENT_SERIAL 20
#define ATA_IDENT_MODEL 54
#define ATA_IDENT_CAPABILITIES 98
#define ATA_IDENT_FIELDVALID 106
#define ATA_IDENT_MAX_LBA 120
#define ATA_IDENT_COMMANDSETS 164
#define ATA_IDENT_MAX_LBA_EXT 200

#define IDE_ATA 0x00
#define IDE_ATAPI 0x01

#define ATA_REG_DATA 0x00
#define ATA_REG_ERROR 0x01
#define ATA_REG_FEATURES 0x01
#define ATA_REG_SECCOUNT0 0x02
#define ATA_REG_LBA0 0x03
#define ATA_REG_LBA1 0x04
#define ATA_REG_LBA2 0x05
#define ATA_REG_HDDEVSEL 0x06
#define ATA_REG_COMMAND 0x07
#define ATA_REG_STATUS 0x07
#define ATA_CTL_CONTROL 0x00
#define ATA_CTL_ALTSTATUS 0x00
#define ATA_CTL_DEVADDRESS 0x01

#define PCI_Mass_Storage_Class 0x1
#define PCI_IDE_Controller_Subclass 0x1

NonnullOwnPtr<IDEChannel> IDEChannel::create(const IDEController& controller, IOAddressGroup io_group, ChannelType type, bool force_pio)
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

IDEChannel::IDEChannel(const IDEController& controller, IOAddressGroup io_group, ChannelType type, bool force_pio)
    : IRQHandler(type == ChannelType::Primary ? PATA_PRIMARY_IRQ : PATA_SECONDARY_IRQ)
    , m_channel_number((type == ChannelType::Primary ? 0 : 1))
    , m_io_group(io_group)
    , m_parent_controller(controller)
{
    disable_irq();

    m_dma_enabled.resource() = !force_pio;
    ProcFS::add_sys_bool("ide_dma", m_dma_enabled);

    initialize(force_pio);
    detect_disks();
    enable_irq();
}

IDEChannel::~IDEChannel()
{
}

void IDEChannel::start_request(AsyncBlockDeviceRequest& request, bool use_dma, bool is_slave)
{
    ScopedSpinLock lock(m_request_lock);
#if PATA_DEBUG
    dbgln("IDEChannel::start_request");
#endif
    m_current_request = &request;
    m_current_request_block_index = 0;
    m_current_request_uses_dma = use_dma;
    m_current_request_flushing_cache = false;

    if (request.request_type() == AsyncBlockDeviceRequest::Read) {
        if (use_dma)
            ata_read_sectors_with_dma(is_slave);
        else
            ata_read_sectors(is_slave);
    } else {
        if (use_dma)
            ata_write_sectors_with_dma(is_slave);
        else
            ata_write_sectors(is_slave);
    }
}

void IDEChannel::complete_current_request(AsyncDeviceRequest::RequestResult result)
{
    // NOTE: this may be called from the interrupt handler!
    ASSERT(m_current_request);
    ASSERT(m_request_lock.is_locked());

    // Now schedule reading back the buffer as soon as we leave the irq handler.
    // This is important so that we can safely write the buffer back,
    // which could cause page faults. Note that this may be called immediately
    // before Processor::deferred_call_queue returns!
    Processor::deferred_call_queue([this, result]() {
        dbgln<PATA_DEBUG>("IDEChannel::complete_current_request result: {}", (int)result);
        ASSERT(m_current_request);
        auto& request = *m_current_request;
        m_current_request = nullptr;

        if (m_current_request_uses_dma) {
            if (result == AsyncDeviceRequest::Success) {
                if (request.request_type() == AsyncBlockDeviceRequest::Read) {
                    if (!request.write_to_buffer(request.buffer(), m_dma_buffer_page->paddr().offset(0xc0000000).as_ptr(), 512 * request.block_count())) {
                        request.complete(AsyncDeviceRequest::MemoryFault);
                        return;
                    }
                }

                // I read somewhere that this may trigger a cache flush so let's do it.
                m_io_group.bus_master_base().offset(2).out<u8>(m_io_group.bus_master_base().offset(2).in<u8>() | 0x6);
            }
        }

        request.complete(result);
    });
}

void IDEChannel::initialize(bool force_pio)
{
    m_parent_controller->enable_pin_based_interrupts();
    if (force_pio) {
        klog() << "IDEChannel: Requested to force PIO mode; not setting up DMA";
        return;
    }

    // Let's try to set up DMA transfers.
    PCI::enable_bus_mastering(m_parent_controller->pci_address());
    m_prdt_page = MM.allocate_supervisor_physical_page();
    prdt().end_of_table = 0x8000;
    m_dma_buffer_page = MM.allocate_supervisor_physical_page();
    klog() << "IDEChannel: Bus master IDE: " << m_io_group.bus_master_base();
}

static void print_ide_status(u8 status)
{
    klog() << "IDEChannel: print_ide_status: DRQ=" << ((status & ATA_SR_DRQ) != 0) << " BSY=" << ((status & ATA_SR_BSY) != 0) << " DRDY=" << ((status & ATA_SR_DRDY) != 0) << " DSC=" << ((status & ATA_SR_DSC) != 0) << " DF=" << ((status & ATA_SR_DF) != 0) << " CORR=" << ((status & ATA_SR_CORR) != 0) << " IDX=" << ((status & ATA_SR_IDX) != 0) << " ERR=" << ((status & ATA_SR_ERR) != 0);
}

void IDEChannel::handle_irq(const RegisterState&)
{
    u8 status = m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>();

    m_entropy_source.add_random_event(status);

    u8 bstatus = m_io_group.bus_master_base().offset(2).in<u8>();
    if (!(bstatus & 0x4)) {
        // interrupt not from this device, ignore
#if PATA_DEBUG
        klog() << "IDEChannel: ignore interrupt";
#endif
        return;
    }

    ScopedSpinLock lock(m_request_lock);
#if PATA_DEBUG
    klog() << "IDEChannel: interrupt: DRQ=" << ((status & ATA_SR_DRQ) != 0) << " BSY=" << ((status & ATA_SR_BSY) != 0) << " DRDY=" << ((status & ATA_SR_DRDY) != 0);
#endif

    if (!m_current_request) {
#if PATA_DEBUG
        dbgln("IDEChannel: IRQ but no pending request!");
#endif
        return;
    }

    bool received_all_irqs = m_current_request_uses_dma || m_current_request_block_index + 1 >= m_current_request->block_count();

    if (status & ATA_SR_ERR) {
        print_ide_status(status);
        m_device_error = m_io_group.io_base().offset(ATA_REG_ERROR).in<u8>();
        dbgln("IDEChannel: Error {:#02x}!", (u8)m_device_error);
        complete_current_request(AsyncDeviceRequest::Failure);
        return;
    }

    m_device_error = 0;
    if (received_all_irqs) {
        complete_current_request(AsyncDeviceRequest::Success);
    } else {
        ASSERT(!m_current_request_uses_dma);

        // Now schedule reading/writing the buffer as soon as we leave the irq handler.
        // This is important so that we can safely access the buffers, which could
        // trigger page faults
        Processor::deferred_call_queue([this]() {
            if (m_current_request->request_type() == AsyncBlockDeviceRequest::Read) {
                dbgln("IDEChannel: Read block {}/{}", m_current_request_block_index, m_current_request->block_count());
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
                    dbgln("IDEChannel: Wrote block {}/{}", m_current_request_block_index, m_current_request->block_count());
                    if (++m_current_request_block_index >= m_current_request->block_count()) {
                        // We read the last block, flush cache
                        ASSERT(!m_current_request_flushing_cache);
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
}

static void io_delay()
{
    for (int i = 0; i < 4; ++i)
        IO::in8(0x3f6);
}

void IDEChannel::detect_disks()
{
    // There are only two possible disks connected to a channel
    for (auto i = 0; i < 2; i++) {
        m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(0xA0 | (i << 4)); // First, we need to select the drive itself

        // Apparently these need to be 0 before sending IDENTIFY?!
        m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(0x00);
        m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>(0x00);
        m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>(0x00);
        m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>(0x00);

        m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(ATA_CMD_IDENTIFY); // Send the ATA_IDENTIFY command

        // Wait for the BSY flag to be reset
        while (m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() & ATA_SR_BSY)
            ;

        if (m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() == 0x00) {
#if PATA_DEBUG
            klog() << "IDEChannel: No " << (i == 0 ? "master" : "slave") << " disk detected!";
#endif
            continue;
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

        u8 cyls = wbufbase[1];
        u8 heads = wbufbase[3];
        u8 spt = wbufbase[6];
        if (cyls == 0 || heads == 0 || spt == 0)
            continue;
        klog() << "IDEChannel: Name=" << ((char*)bbuf.data() + 54) << ", C/H/Spt=" << cyls << "/" << heads << "/" << spt;

        if (i == 0) {
            m_master = PATADiskDevice::create(m_parent_controller, *this, PATADiskDevice::DriveType::Master, cyls, heads, spt, 3, (m_channel_number == 0) ? 0 : 2);
        } else {
            m_slave = PATADiskDevice::create(m_parent_controller, *this, PATADiskDevice::DriveType::Slave, cyls, heads, spt, 3, (m_channel_number == 0) ? 1 : 3);
        }
    }
}

void IDEChannel::ata_read_sectors_with_dma(bool slave_request)
{
    auto& request = *m_current_request;
    u32 lba = request.block_index();
    dbgln<PATA_DEBUG>("IDEChannel::ata_read_sectors_with_dma ({} x {})", lba, request.block_count());

    prdt().offset = m_dma_buffer_page->paddr();
    prdt().size = 512 * request.block_count();

    ASSERT(prdt().size <= PAGE_SIZE);

    // Stop bus master
    m_io_group.bus_master_base().out<u8>(0);

    // Write the PRDT location
    m_io_group.bus_master_base().offset(4).out(m_prdt_page->paddr().get());

    // Turn on "Interrupt" and "Error" flag. The error flag should be cleared by hardware.
    m_io_group.bus_master_base().offset(2).out<u8>(m_io_group.bus_master_base().offset(2).in<u8>() | 0x6);

    // Set transfer direction
    m_io_group.bus_master_base().out<u8>(0x8);

    while (m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() & ATA_SR_BSY)
        ;

    m_io_group.control_base().offset(ATA_CTL_CONTROL).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(0x40 | (static_cast<u8>(slave_request) << 4));
    io_delay();

    m_io_group.io_base().offset(ATA_REG_FEATURES).out<u16>(0);

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>(0);

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(request.block_count());
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>((lba & 0x000000ff) >> 0);
    m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>((lba & 0x0000ff00) >> 8);
    m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>((lba & 0x00ff0000) >> 16);

    for (;;) {
        auto status = m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>();
        if (!(status & ATA_SR_BSY) && (status & ATA_SR_DRDY))
            break;
    }

    m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(ATA_CMD_READ_DMA_EXT);
    io_delay();

    enable_irq();
    // Start bus master
    m_io_group.bus_master_base().out<u8>(0x9);
}

bool IDEChannel::ata_do_read_sector()
{
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

void IDEChannel::ata_read_sectors(bool slave_request)
{
    auto& request = *m_current_request;
    ASSERT(request.block_count() <= 256);
#if PATA_DEBUG
    dbgln("IDEChannel::ata_read_sectors");
#endif

    while (m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() & ATA_SR_BSY)
        ;

    auto lba = request.block_index();
#if PATA_DEBUG
    klog() << "IDEChannel: Reading " << request.block_count() << " sector(s) @ LBA " << lba;
#endif

    u8 devsel = 0xe0;
    if (slave_request)
        devsel |= 0x10;

    m_io_group.control_base().offset(ATA_CTL_CONTROL).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(devsel | (static_cast<u8>(slave_request) << 4) | 0x40);
    io_delay();

    m_io_group.io_base().offset(ATA_REG_FEATURES).out<u8>(0);

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>(0);

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(request.block_count());
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>((lba & 0x000000ff) >> 0);
    m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>((lba & 0x0000ff00) >> 8);
    m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>((lba & 0x00ff0000) >> 16);

    for (;;) {
        auto status = m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>();
        if (!(status & ATA_SR_BSY) && (status & ATA_SR_DRDY))
            break;
    }

    enable_irq();
    m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(ATA_CMD_READ_PIO);
}

void IDEChannel::ata_write_sectors_with_dma(bool slave_request)
{
    auto& request = *m_current_request;
    u32 lba = request.block_index();
    dbgln<PATA_DEBUG>("IDEChannel::ata_write_sectors_with_dma ({} x {})", lba, request.block_count());

    prdt().offset = m_dma_buffer_page->paddr();
    prdt().size = 512 * request.block_count();

    if (!request.read_from_buffer(request.buffer(), m_dma_buffer_page->paddr().offset(0xc0000000).as_ptr(), 512 * request.block_count())) {
        complete_current_request(AsyncDeviceRequest::MemoryFault);
        return;
    }

    ASSERT(prdt().size <= PAGE_SIZE);

    // Stop bus master
    m_io_group.bus_master_base().out<u8>(0);

    // Write the PRDT location
    m_io_group.bus_master_base().offset(4).out<u32>(m_prdt_page->paddr().get());

    // Turn on "Interrupt" and "Error" flag. The error flag should be cleared by hardware.
    m_io_group.bus_master_base().offset(2).out<u8>(m_io_group.bus_master_base().offset(2).in<u8>() | 0x6);

    while (m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() & ATA_SR_BSY)
        ;

    m_io_group.control_base().offset(ATA_CTL_CONTROL).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(0x40 | (static_cast<u8>(slave_request) << 4));
    io_delay();

    m_io_group.io_base().offset(ATA_REG_FEATURES).out<u16>(0);

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>(0);
    m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>(0);

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(request.block_count());
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>((lba & 0x000000ff) >> 0);
    m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>((lba & 0x0000ff00) >> 8);
    m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>((lba & 0x00ff0000) >> 16);

    for (;;) {
        auto status = m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>();
        if (!(status & ATA_SR_BSY) && (status & ATA_SR_DRDY))
            break;
    }

    m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(ATA_CMD_WRITE_DMA_EXT);
    io_delay();

    enable_irq();
    // Start bus master
    m_io_group.bus_master_base().out<u8>(0x1);
}

void IDEChannel::ata_do_write_sector()
{
    auto& request = *m_current_request;

    io_delay();
    while ((m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() & ATA_SR_BSY) || !(m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() & ATA_SR_DRQ))
        ;

    u8 status = m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>();
    ASSERT(status & ATA_SR_DRQ);

    auto in_buffer = request.buffer().offset(m_current_request_block_index * 512);
#ifndef PATA_DEBUG
    dbgln("IDEChannel: Writing 512 bytes (part {}) (status={:#02x})...", m_current_request_block_index, status);
#endif
    ssize_t nread = request.read_from_buffer_buffered<512>(in_buffer, 512, [&](const u8* buffer, size_t buffer_bytes) {
        for (size_t i = 0; i < buffer_bytes; i += sizeof(u16))
            IO::out16(m_io_group.io_base().offset(ATA_REG_DATA).get(), *(const u16*)&buffer[i]);
        return (ssize_t)buffer_bytes;
    });
    if (nread < 0)
        complete_current_request(AsyncDeviceRequest::MemoryFault);
}

void IDEChannel::ata_write_sectors(bool slave_request)
{
    auto& request = *m_current_request;

    ASSERT(request.block_count() <= 256);
    u32 start_sector = request.block_index();
    u32 count = request.block_count();
#if PATA_DEBUG
    klog() << "IDEChannel::ata_write_sectors request (" << count << " sector(s) @ " << start_sector << ")";
#endif

    while (m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() & ATA_SR_BSY)
        ;

#if PATA_DEBUG
    klog() << "IDEChannel: Writing " << count << " sector(s) @ LBA " << start_sector;
#endif

    u8 devsel = 0xe0;
    if (slave_request)
        devsel |= 0x10;

    m_io_group.io_base().offset(ATA_REG_SECCOUNT0).out<u8>(count == 256 ? 0 : LSB(count));
    m_io_group.io_base().offset(ATA_REG_LBA0).out<u8>(start_sector & 0xff);
    m_io_group.io_base().offset(ATA_REG_LBA1).out<u8>((start_sector >> 8) & 0xff);
    m_io_group.io_base().offset(ATA_REG_LBA2).out<u8>((start_sector >> 16) & 0xff);
    m_io_group.io_base().offset(ATA_REG_HDDEVSEL).out<u8>(devsel | ((start_sector >> 24) & 0xf));

    IO::out8(0x3F6, 0x08);
    while (!(m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() & ATA_SR_DRDY))
        ;

    m_io_group.io_base().offset(ATA_REG_COMMAND).out<u8>(ATA_CMD_WRITE_PIO);

    io_delay();
    while ((m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() & ATA_SR_BSY) || !(m_io_group.io_base().offset(ATA_REG_STATUS).in<u8>() & ATA_SR_DRQ))
        ;

    enable_irq();
    ata_do_write_sector();
}

}
