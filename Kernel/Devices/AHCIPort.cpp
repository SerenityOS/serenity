/*
* Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
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

#include <AK/Memory.h>
#include <Kernel/Devices/AHCIController.h>
#include <Kernel/Devices/AHCIPort.h>
#include <Kernel/VM/MemoryManager.h>

#define FIS_TYPE_REG_H2D 0x27
#define FIS_TYPE_PIO_SETUP 0x5F

#define DEV_CLB 0x00
#define DEV_CLBU 0x04
#define DEV_FB 0x08
#define DEV_FBU 0x0C
#define DEV_IS 0x10
#define DEV_IE 0x14
#define DEV_CMD 0x18
#define DEV_TFD 0x20
#define DEV_SIG 0x24
#define DEV_SSTS 0x28
#define DEV_SERR 0x30
#define DEV_SACT 0x34
#define DEV_CI 0x38

#define CMD_PRDTL 0x02
#define CMD_PRDBC 0x04
#define CMD_CTBA 0x08
#define CMD_CTBAU 0x0C

#define IRQ_TFES 0x1 << 30
#define IRQ_HBFS 0x1 << 29
#define IRQ_HBDS 0x1 << 28
#define IRQ_IFS 0x1 << 27
#define IRQ_INFS 0x1 << 26
#define IRQ_OFS 0x1 << 24

#define SATA_SIG_ATA 0x00000101
#define SATA_SIG_ATAPI 0xEB140101
#define SATA_SIG_SEMB 0xC33C0101
#define SATA_SIG_PM 0x96690101

#define HBA_PORT_IPM_ACTIVE 1
#define HBA_PORT_DET_PRESENT 3

#define ATA_DEV_BUSY 0x80
#define ATA_DEV_DRQ 0x08
#define ATA_CMD_READ_DMA_EX 0x25
#define ATA_CMD_IDENTIFY 0xEC

#define HBA_PxCMD_ST 0x0001
#define HBA_PxCMD_FRE 0x0010
#define HBA_PxCMD_FR 0x4000
#define HBA_PxCMD_CR 0x8000
#define HBA_PxIS_TFES 1 << 30

#define SLOT_CFL 0x1F
#define SLOT_ATAPI 0x1 << 5
#define SLOT_WRITE 0x1 << 6
#define SLOT_PREFETCH 0x1 << 7

#define SLOT_RESET 0x1 << 8
#define SLOT_BIST 0x1 << 9
#define SLOT_CLEAR 0x1 << 10
#define SLOT_PORT_MULT 0xF000
#define DATA_ENTRY_DBC 0x3fffff
#define DATA_ENTRY_I 0x1 << 31
#define FIS_H2D_PMPORT 0xF
#define FIS_H2D_MODE 0x1 << 7

#define LSB(x) ((x)&0xFF)
#define MSB(x) (((x) >> 8) & 0xFF)

// Only store at least one page inside the buffer
#define SECTOR_SIZE 512
#define MAX_SECTORS (PAGE_SIZE / SECTOR_SIZE) + 1
#define BUFFER_SIZE MAX_SECTORS* SECTOR_SIZE

#define COMMAND_TABLE_SIZE 256
#define COMMAND_LIST_SIZE 1024                    // 1K
#define FIS_SIZE 1024 * 4                         // 4K
#define COMMAND_SLOT_SIZE COMMAND_TABLE_SIZE * 32 // 8K

namespace Kernel {

struct FISRegHostToDevice {
    u8 fis_type;

    u8 byte1;
    u8 command;
    u8 featurel;

    u8 lba0;
    u8 lba1;
    u8 lba2;
    u8 device;

    u8 lba3;
    u8 lba4;
    u8 lba5;
    u8 featureh;

    u8 countl;
    u8 counth;
    u8 icc;
    u8 control;

    u8 rsv1[4];
};

AHCISlot::AHCISlot(VirtualAddress base, AHCIPort& port, VirtualAddress buffer, PhysicalAddress buffer_physical, u32 index)
    : m_base_addr(base)
    , m_buffer(buffer)
    , m_buffer_physical(buffer_physical)
    , m_index(index)
{
    m_reg = (SlotReg*)m_base_addr.get();
    rebase(port);
}

void AHCISlot::rebase(AHCIPort& port)
{
    m_reg->prdtl = 8;

    // Command Table
    // Offset:      5K + (256 * slot)
    // Size:		256
    // Max Count:	1
    // Max Size:	256 * 1 (256)
    m_ctba_offset = (COMMAND_LIST_SIZE + FIS_SIZE) + (COMMAND_TABLE_SIZE * m_index);
    m_reg->ctba = port.base_physical() + m_ctba_offset;
    m_reg->ctbau = 0;

    auto table = (u8*)(port.base() + m_ctba_offset);
    m_cfis = table + 0x00;
    m_acmd = table + 0x40;
    m_entries = (DataEntry*)(table + 0x80);
    memset(table, 0, COMMAND_TABLE_SIZE);
}

void AHCISlot::setup_command(AHCIPort& port)
{
    // Reset command table
    memset((void*)(port.base() + m_ctba_offset), 0,
        256);
}

bool AHCISlot::execute_command(AHCIPort& port)
{
    // Wait until port is not busy
    u32 timeout = 0;
    while (port.is_busy() && timeout < 1000000)
        timeout += 1;

    if (timeout == 1000000) {
        dbg() << "AHCI: Port " << port.index() << " timed out\n";
        ASSERT_NOT_REACHED();
        return false;
    }

    // Issue the command
    port.issue_command(m_index);

    // Wait for command to be done
    do {
        if (port.has_file_error()) {
            dbg() << "AHCI: Disk read error\n";
            return false;
        }
    } while (port.is_executing(m_index));

    return true;
}

bool AHCISlot::issue_identify(AHCIPort& port, u8* out)
{
    setup_command(port);
    m_reg->word0 = 0;
    m_reg->word0 |= (sizeof(FISRegHostToDevice) / sizeof(u32)) & SLOT_CFL;
    m_reg->word0 |= 0x0 & SLOT_WRITE;
    m_reg->prdtl = 1;

    m_entries[0].dba = m_buffer_physical.get();
    m_entries[0].dbau = 0;
    m_entries[0].dword3 = 0;
    m_entries[0].dword3 |= (256 * 2) & DATA_ENTRY_DBC;
    m_entries[0].dword3 |= DATA_ENTRY_I;

    memset((void*)m_cfis, 0, sizeof(FISRegHostToDevice));
    auto cmd = (FISRegHostToDevice*)m_cfis;
    cmd->fis_type = FIS_TYPE_REG_H2D;
    cmd->command = ATA_CMD_IDENTIFY;
    cmd->device = m_index;
    cmd->byte1 = FIS_H2D_MODE;

    bool success = execute_command(port);
    memcpy(out, (void*)m_buffer.get(), 256 * 2);
    return success;
}

bool AHCISlot::issue_read(AHCIPort& port, u32 start, u16 count, u8* out)
{
    setup_command(port);
    u16 entry_count = ((count - 1) >> 4) + 1;
    m_reg->word0 |= (sizeof(FISRegHostToDevice) / sizeof(u32)) & SLOT_CFL;
    m_reg->word0 |= 0 & SLOT_WRITE;
    m_reg->word0 |= SLOT_CLEAR;
    m_reg->word0 |= 0 & SLOT_PREFETCH;
    m_reg->prdtl = entry_count;

    // 8K bytes (16 sectors) per table entry
    int count_left = count;
    int buffer_offset = 0;
    for (int i = 0; i < entry_count - 1; i++) {
        m_entries[i].dba = m_buffer_physical.get() + buffer_offset;
        m_entries[i].dbau = 0;
        m_entries[i].dword3 = 0;
        m_entries[i].dword3 |= (8 * 1024 - 1) & DATA_ENTRY_DBC;
        m_entries[i].dword3 |= DATA_ENTRY_I;
        out += 4 * 1024;
        buffer_offset -= 16;
    }

    // Make up for the remaining space with the last entry
    u16 last = entry_count - 1;
    m_entries[last].dba = m_buffer_physical.get() + buffer_offset;
    m_entries[last].dbau = 0;
    m_entries[last].dword3 = 0;
    m_entries[last].dword3 |= ((count_left << 9) - 1) & DATA_ENTRY_DBC;
    m_entries[last].dword3 |= DATA_ENTRY_I;

    // Build command
    memset((void*)m_cfis, 0, sizeof(FISRegHostToDevice));
    auto cmd = (FISRegHostToDevice*)m_cfis;
    cmd->fis_type = FIS_TYPE_REG_H2D;
    cmd->command = ATA_CMD_READ_DMA_EX;
    cmd->byte1 = FIS_H2D_MODE;

    cmd->lba0 = (u8)(start >> 0) & 0xFF;
    cmd->lba1 = (u8)(start >> 8) & 0xFF;
    cmd->lba2 = (u8)(start >> 16) & 0xFF;
    cmd->lba3 = (u8)(start >> 24) & 0xFF;
    cmd->lba4 = (u8)(0);
    cmd->lba5 = (u8)(0);

    cmd->device = 1 << 6; // LBA mode
    cmd->countl = (count >> 0) & 0xFF;
    cmd->counth = (count >> 8) & 0xFF;

    bool success = execute_command(port);
    memcpy(out, (void*)m_buffer.get(), 512 * count);
    return success;
}

AHCIPort::AHCIPort(VirtualAddress addr, u32 index)
    : m_base_addr(addr)
    , m_index(index)
{
    // Assign data pointers
    m_reg = (PortReg*)(m_base_addr.get());

    m_type = check_type();
    if (m_type == Type::SATA) {
        m_disk_device = adopt(*new AHCIDiskDevice(
            *this, 8, m_index * 16));

        rebase();
        identify_device();
    }
}

bool AHCIPort::handle_irq()
{
    u32 is = m_reg->is;

    // Output any errors
    if (is & IRQ_TFES)
        klog() << "ACHIPort: Task file error";
    if (is & IRQ_HBFS)
        klog() << "ACHIPort: Host bus fatal error";
    if (is & IRQ_HBDS)
        klog() << "ACHIPort: Host bus data error";
    if (is & IRQ_IFS)
        klog() << "ACHIPort: Interface fatal error";
    if (is & IRQ_INFS)
        klog() << "ACHIPort: Interface non-fatal error";
    if (is & IRQ_OFS)
        klog() << "ACHIPort: Overflow in data table";

    // If there's a fatal error, return true
    if (is & IRQ_HBFS || is & IRQ_IFS)
        return true;

    return false;
}

void AHCIPort::identify_device()
{
    auto& slot = find_free_slot();
    u16* wbuf = (u16*)kmalloc(256 * 2);
    slot.issue_identify(*this, (u8*)wbuf);

    u8* bbuf = (u8*)kmalloc(256 * 2);
    for (int i = 0; i < 256; i++) {
        bbuf[i * 2 + 0] = MSB(wbuf[i]);
        bbuf[i * 2 + 1] = LSB(wbuf[i]);
    }

    // "Unpad" the device name string.
    for (u32 i = 93; i > 54 && bbuf[i] == ' '; --i)
        bbuf[i] = 0;

    auto name = String((const char*)bbuf + 54, strlen((const char*)bbuf + 54));
    u16 cylinders = wbuf[1];
    u16 heads = wbuf[3];
    u16 sectors_per_track = wbuf[6];
    m_disk_device->set_drive_geometry(cylinders, heads, sectors_per_track);

    klog() << "ACHIPort: Index="
           << m_index << ", Name=" << name
           << "C/H/Spt=" << cylinders << "/" << heads << "/" << sectors_per_track;

    kfree(wbuf);
    kfree(bbuf);
}

void AHCIPort::rebase()
{
    // Pause command exection while we rebase
    stop_command_execution();

    // Allocate new memory region for rebase
    // Command List (1K) + FIS (4K) + Command Table (8K) = 13K
    m_ahci_base_region = MM.allocate_contiguous_kernel_region(
        PAGE_ROUND_UP(COMMAND_LIST_SIZE + FIS_SIZE + COMMAND_SLOT_SIZE),
        "AHCI Base", Region::Access::Read | Region::Access::Write);
    m_ahci_base = m_ahci_base_region->vaddr();
    m_ahci_base_physical = m_ahci_base_region->vmobject()
                               .physical_pages()[0]
                               ->paddr();

    // Map a new region of continues memory for the buffer to go
    m_buffer_region = MM.allocate_contiguous_kernel_region(
        PAGE_ROUND_UP(BUFFER_SIZE), "Data Buffer",
        Region::Access::Read | Region::Access::Write);
    m_buffer = m_buffer_region->vaddr();
    m_buffer_physical = m_buffer_region->vmobject().physical_pages()[0]->paddr();

#ifdef DEBUG_AHCI
    klog() << "AHCIDiskDevice: Create buffer at Virtual Address "
           << m_buffer.get() << " and Physical Address "
           << m_buffer_physical.get();
#endif

    // Command List
    // Size:		32
    // Max Count:	32
    // Max Size:	32 * 32 (1K)
    m_clb_offset = 0;
    m_reg->clb = m_ahci_base_physical.get() + m_clb_offset;
    m_reg->clbu = 0;
    memset((void*)(m_ahci_base.get() + m_clb_offset), 0, COMMAND_LIST_SIZE);
#ifdef DEBUG_AHCI
    klog() << "AHCIPort: Rebased command table to: 0x" << m_reg->clb;
#endif

    // FIS
    // Offset:		1K
    // Size:		4K
    m_fb_offset = COMMAND_LIST_SIZE;
    m_reg->fb = m_ahci_base_physical.get() + m_fb_offset;
    m_reg->fbu = 0;
    memset((void*)(m_ahci_base.get() + m_fb_offset), 0, FIS_SIZE);
#ifdef DEBUG_AHCI
    klog() << "AHCIPort: Rebased FB to: 0x" << m_reg->fb;
#endif

    // Command Table
    // Offset: 		5K
    // Size:		256 * 32 (8K)
    for (u32 i = 0; i < 32; i++) {
        auto cmd_addr = m_ahci_base.offset(
            m_clb_offset + 0x20 * i);

        m_commands[i] = AHCISlot(cmd_addr, *this,
            m_buffer, m_buffer_physical, i);
    }
#ifdef DEBUG_AHCI
    klog() << "AHCIPort: Commands";
#endif

    start_command_execution();
}

void AHCIPort::stop_command_execution()
{
    // Clear ST
    m_reg->is = 0;
    m_reg->cmd = 0;
    m_reg->cmd &= ~HBA_PxCMD_ST;
    m_reg->cmd &= ~HBA_PxCMD_FRE;

    // Wait until both FR and CR bits are clear
    while ((m_reg->cmd & HBA_PxCMD_CR || m_reg->cmd & HBA_PxCMD_FR)
        && !AHCIController::the().has_fatal_error()) {
        continue;
    }
}

void AHCIPort::start_command_execution()
{
    // Wait until CR is clear
    while (m_reg->cmd & HBA_PxCMD_CR
        && !AHCIController::the().has_fatal_error()) {
        continue;
    }

    // Set FRE and ST
    m_reg->cmd |= HBA_PxCMD_FRE;
    m_reg->cmd |= HBA_PxCMD_ST;

    m_reg->is = 0;
    m_reg->ie = (u32)-1;
}

AHCIPort::Type AHCIPort::check_type()
{
    u8 ipm = (m_reg->ssts >> 8) & 0x0F;
    u8 det = m_reg->ssts & 0x0F;

    if (det != HBA_PORT_DET_PRESENT
        || ipm != HBA_PORT_IPM_ACTIVE) {
        return Type::UNKOWN;
    }

    switch (m_reg->sig) {
    case SATA_SIG_ATAPI:
        return Type::SATAPI;
    case SATA_SIG_SEMB:
        return Type::SEMB;
    case SATA_SIG_PM:
        return Type::PM;
    default:
        return Type::SATA;
    }
}

AHCISlot& AHCIPort::find_free_slot()
{
    // If both the SACT and CI are not set,
    // then the slot if free for new commands
    u32 slots = m_reg->sact | m_reg->ci;
    for (u32 i = 0; i < 32; i++) {
        if ((slots & 1) == 0)
            return m_commands[i];

        slots >>= 1;
    }

    dbg() << "AHCI: No free slots where found\n";
    ASSERT_NOT_REACHED();
}

bool AHCIPort::is_busy()
{
    return m_reg->tfd & (ATA_DEV_BUSY | ATA_DEV_DRQ);
}

void AHCIPort::issue_command(u32 slot)
{
    m_reg->is = 0;
    m_reg->ci = 1 << slot;
}

bool AHCIPort::is_executing(u32 slot)
{
    return (m_reg->ci & (1 << slot)) != 0;
}

bool AHCIPort::has_file_error()
{
    return m_reg->is & HBA_PxIS_TFES;
}

}
