#include "IDEDiskDevice.h"
#include <AK/Types.h>
#include "Process.h"
#include "StdLib.h"
#include "IO.h"
#include "Scheduler.h"
#include "PIC.h"
#include <Kernel/Lock.h>

//#define DISK_DEBUG

#define IRQ_FIXED_DISK 14

#define IDE0_DATA        0x1F0
#define IDE0_STATUS      0x1F7
#define IDE0_COMMAND     0x1F7

#define ATA_SR_BSY     0x80
#define ATA_SR_DRDY    0x40
#define ATA_SR_DF      0x20
#define ATA_SR_DSC     0x10
#define ATA_SR_DRQ     0x08
#define ATA_SR_CORR    0x04
#define ATA_SR_IDX     0x02
#define ATA_SR_ERR     0x01

#define ATA_ER_BBK      0x80
#define ATA_ER_UNC      0x40
#define ATA_ER_MC       0x20
#define ATA_ER_IDNF     0x10
#define ATA_ER_MCR      0x08
#define ATA_ER_ABRT     0x04
#define ATA_ER_TK0NF    0x02
#define ATA_ER_AMNF     0x01

#define ATA_CMD_READ_PIO          0x20
#define ATA_CMD_READ_PIO_EXT      0x24
#define ATA_CMD_READ_DMA          0xC8
#define ATA_CMD_READ_DMA_EXT      0x25
#define ATA_CMD_WRITE_PIO         0x30
#define ATA_CMD_WRITE_PIO_EXT     0x34
#define ATA_CMD_WRITE_DMA         0xCA
#define ATA_CMD_WRITE_DMA_EXT     0x35
#define ATA_CMD_CACHE_FLUSH       0xE7
#define ATA_CMD_CACHE_FLUSH_EXT   0xEA
#define ATA_CMD_PACKET            0xA0
#define ATA_CMD_IDENTIFY_PACKET   0xA1
#define ATA_CMD_IDENTIFY          0xEC

#define ATAPI_CMD_READ            0xA8
#define ATAPI_CMD_EJECT           0x1B

#define ATA_IDENT_DEVICETYPE   0
#define ATA_IDENT_CYLINDERS    2
#define ATA_IDENT_HEADS        6
#define ATA_IDENT_SECTORS      12
#define ATA_IDENT_SERIAL       20
#define ATA_IDENT_MODEL        54
#define ATA_IDENT_CAPABILITIES 98
#define ATA_IDENT_FIELDVALID   106
#define ATA_IDENT_MAX_LBA      120
#define ATA_IDENT_COMMANDSETS  164
#define ATA_IDENT_MAX_LBA_EXT  200

#define IDE_ATA            0x00
#define IDE_ATAPI          0x01

#define ATA_REG_DATA       0x00
#define ATA_REG_ERROR      0x01
#define ATA_REG_FEATURES   0x01
#define ATA_REG_SECCOUNT0  0x02
#define ATA_REG_LBA0       0x03
#define ATA_REG_LBA1       0x04
#define ATA_REG_LBA2       0x05
#define ATA_REG_HDDEVSEL   0x06
#define ATA_REG_COMMAND    0x07
#define ATA_REG_STATUS     0x07
#define ATA_REG_SECCOUNT1  0x08
#define ATA_REG_LBA3       0x09
#define ATA_REG_LBA4       0x0A
#define ATA_REG_LBA5       0x0B
#define ATA_REG_CONTROL    0x0C
#define ATA_REG_ALTSTATUS  0x0C
#define ATA_REG_DEVADDRESS 0x0D

enum IDECommand : byte {
    IDENTIFY_DRIVE = 0xEC,
    READ_SECTORS = 0x21,
    WRITE_SECTORS = 0x30,
    FLUSH_CACHE = 0xe7,
};

enum IDEStatus : byte {
    BUSY = (1 << 7),
    DRDY = (1 << 6),
    DF   = (1 << 5),
    SRV  = (1 << 4),
    DRQ  = (1 << 3),
    CORR = (1 << 2),
    IDX  = (1 << 1),
    ERR  = (1 << 0),
};

Retained<IDEDiskDevice> IDEDiskDevice::create()
{
    return adopt(*new IDEDiskDevice);
}

IDEDiskDevice::IDEDiskDevice()
    : IRQHandler(IRQ_FIXED_DISK)
{
    initialize();
}

IDEDiskDevice::~IDEDiskDevice()
{
}

const char* IDEDiskDevice::class_name() const
{
    return "IDEDiskDevice";
}

unsigned IDEDiskDevice::block_size() const
{
    return 512;
}

bool IDEDiskDevice::read_block(unsigned index, byte* out) const
{
    if (m_bus_master_base)
        return const_cast<IDEDiskDevice&>(*this).read_sector_with_dma(index, out);
    return const_cast<IDEDiskDevice&>(*this).read_sectors(index, 1, out);
}

bool IDEDiskDevice::write_block(unsigned index, const byte* data)
{
    return write_sectors(index, 1, data);
}

static void print_ide_status(byte status)
{
    kprintf("DRQ=%u BUSY=%u DRDY=%u SRV=%u DF=%u CORR=%u IDX=%u ERR=%u\n",
            (status & DRQ) != 0,
            (status & BUSY) != 0,
            (status & DRDY) != 0,
            (status & SRV) != 0,
            (status & DF) != 0,
            (status & CORR) != 0,
            (status & IDX) != 0,
            (status & ERR) != 0);
}

bool IDEDiskDevice::wait_for_irq()
{
#ifdef DISK_DEBUG
    kprintf("disk: waiting for interrupt...\n");
#endif
    // FIXME: Add timeout.
    while (!m_interrupted) {
        // FIXME: Put this process into a Blocked state instead, it's stupid to wake up just to check a flag.
        Scheduler::yield();
    }
#ifdef DISK_DEBUG
    kprintf("disk: got interrupt!\n");
#endif
    memory_barrier();
    return true;
}

void IDEDiskDevice::handle_irq()
{
    byte status = IO::in8(0x1f7);
    if (status & ERR) {
        print_ide_status(status);
        m_device_error = IO::in8(0x1f1);
        kprintf("IDEDiskDevice: Error %b!\n", m_device_error);
    } else {
        m_device_error = 0;
    }
#ifdef DISK_DEBUG
    kprintf("disk:interrupt: DRQ=%u BUSY=%u DRDY=%u\n", (status & DRQ) != 0, (status & BUSY) != 0, (status & DRDY) != 0);
#endif
    m_interrupted = true;
}

void IDEDiskDevice::initialize()
{
    static const PCI::ID piix3_ide_id = { 0x8086, 0x7010 };
    static const PCI::ID piix4_ide_id = { 0x8086, 0x7111 };
    PCI::enumerate_all([this] (const PCI::Address& address, PCI::ID id) {
        if (id == piix3_ide_id || id == piix4_ide_id) {
            m_pci_address = address;
            kprintf("PIIX%u IDE device found!\n", id == piix3_ide_id ? 3 : 4);
        }
    });

#ifdef DISK_DEBUG
    byte status = IO::in8(IDE0_STATUS);
    kprintf("initial status: ");
    print_ide_status(status);
#endif

    m_interrupted = false;

    while (IO::in8(IDE0_STATUS) & BUSY);

    enable_irq();

    IO::out8(0x1F6, 0xA0); // 0xB0 for 2nd device
    IO::out8(0x3F6, 0xA0); // 0xB0 for 2nd device
    IO::out8(IDE0_COMMAND, IDENTIFY_DRIVE);

    enable_irq();
    wait_for_irq();

    ByteBuffer wbuf = ByteBuffer::create_uninitialized(512);
    ByteBuffer bbuf = ByteBuffer::create_uninitialized(512);
    byte* b = bbuf.pointer();
    word* w = (word*)wbuf.pointer();
    const word* wbufbase = (word*)wbuf.pointer();

    for (dword i = 0; i < 256; ++i) {
        word data = IO::in16(IDE0_DATA);
        *(w++) = data;
        *(b++) = MSB(data);
        *(b++) = LSB(data);
    }

    // "Unpad" the device name string.
    for (dword i = 93; i > 54 && bbuf[i] == ' '; --i)
        bbuf[i] = 0;

    m_cylinders = wbufbase[1];
    m_heads = wbufbase[3];
    m_sectors_per_track = wbufbase[6];

    kprintf(
        "IDEDiskDevice: Master=\"%s\", C/H/Spt=%u/%u/%u\n",
        bbuf.pointer() + 54,
        m_cylinders,
        m_heads,
        m_sectors_per_track
    );

    // Let's try to set up DMA transfers.
    if (!m_pci_address.is_null()) {
        m_prdt.end_of_table = 0x8000;
        PCI::enable_bus_mastering(m_pci_address);
        m_bus_master_base = PCI::get_BAR4(m_pci_address) & 0xfffc;
        dbgprintf("PIIX Bus master IDE: I/O @ %x\n", m_bus_master_base);
    }
}

static void wait_400ns(word io_base)
{
    for (int i = 0; i < 4; ++i)
        IO::in8(io_base + ATA_REG_ALTSTATUS);
}

bool IDEDiskDevice::read_sector_with_dma(dword lba, byte* outbuf)
{
    LOCKER(m_lock);
#ifdef DISK_DEBUG
    dbgprintf("%s(%u): IDEDiskDevice::read_sector_with_dma (%u) -> %p\n",
            current->process().name().characters(),
            current->pid(), lba, outbuf);
#endif

    disable_irq();

    m_prdt.offset = PhysicalAddress((dword)outbuf);
    m_prdt.size = 512;

    // Stop bus master
    IO::out8(m_bus_master_base, 0);

    // Write the PRDT location
    IO::out32(m_bus_master_base + 4, (dword)&m_prdt);

    // Turn on "Interrupt" and "Error" flag. The error flag should be cleared by hardware.
    IO::out8(m_bus_master_base + 2, IO::in8(m_bus_master_base + 2) | 0x6);

    // Set transfer direction
    IO::out8(m_bus_master_base, 0x8);

    m_interrupted = false;
    enable_irq();

    while (IO::in8(IDE0_STATUS) & BUSY);

    word io_base = 0x1f0;
    bool is_slave = false;

    IO::out8(io_base + ATA_REG_CONTROL, 0);
    IO::out8(io_base + ATA_REG_HDDEVSEL, 0xe0 | (is_slave << 4));
    wait_400ns(io_base);

    IO::out8(io_base + ATA_REG_FEATURES, 0);

    IO::out8(io_base + ATA_REG_SECCOUNT0, 0);
    IO::out8(io_base + ATA_REG_LBA0, 0);
    IO::out8(io_base + ATA_REG_LBA1, 0);
    IO::out8(io_base + ATA_REG_LBA2, 0);

    IO::out8(io_base + ATA_REG_SECCOUNT0, 1);
    IO::out8(io_base + ATA_REG_LBA0, (lba & 0x000000ff) >> 0);
    IO::out8(io_base + ATA_REG_LBA1, (lba & 0x0000ff00) >> 8);
    IO::out8(io_base + ATA_REG_LBA2, (lba & 0x00ff0000) >> 16);

    for (;;) {
        auto status = IO::in8(io_base + ATA_REG_STATUS);
        if (!(status & ATA_SR_BSY) && (status & ATA_SR_DRDY))
            break;
    }

    IO::out8(io_base + ATA_REG_COMMAND, ATA_CMD_READ_DMA_EXT);
    wait_400ns(io_base);

    // Start bus master
    IO::out8(m_bus_master_base, 0x9);

    for (;;) {
        auto status = IO::in8(m_bus_master_base + 2);
        auto dstatus = IO::in8(io_base + ATA_REG_STATUS);
        if (!(status & 4))
            continue;
        if (!(dstatus & ATA_SR_BSY))
            break;
    }

    disable_irq();

    // I read somewhere that this may trigger a cache flush so let's do it.
    IO::out8(m_bus_master_base + 2, IO::in8(m_bus_master_base + 2) | 0x6);
    return true;
}

bool IDEDiskDevice::read_sectors(dword start_sector, word count, byte* outbuf)
{
    ASSERT(count <= 256);
    LOCKER(m_lock);
#ifdef DISK_DEBUG
    dbgprintf("%s: Disk::read_sectors request (%u sector(s) @ %u)\n",
            current->process().name().characters(),
            count,
            start_sector);
#endif
    disable_irq();

    while (IO::in8(IDE0_STATUS) & BUSY);

#ifdef DISK_DEBUG
    kprintf("IDEDiskDevice: Reading %u sector(s) @ LBA %u\n", count, start_sector);
#endif

    IO::out8(0x1f2, count == 256 ? 0 : LSB(count));
    IO::out8(0x1f3, start_sector & 0xff);
    IO::out8(0x1f4, (start_sector >> 8) & 0xff);
    IO::out8(0x1f5, (start_sector >> 16) & 0xff);
    IO::out8(0x1f6, 0xe0 | ((start_sector >> 24) & 0xf)); // 0xf0 for 2nd device

    IO::out8(0x3F6, 0x08);
    while (!(IO::in8(IDE0_STATUS) & DRDY));

    IO::out8(IDE0_COMMAND, READ_SECTORS);
    m_interrupted = false;
    enable_irq();
    wait_for_irq();

    if (m_device_error)
        return false;

    byte status = IO::in8(0x1f7);
    ASSERT(status & DRQ);
#ifdef DISK_DEBUG
    kprintf("Retrieving %u bytes (status=%b), outbuf=%p...\n", count * 512, status, outbuf);
#endif

    IO::repeated_in16(IDE0_DATA, outbuf, count * 256);
    return true;
}

bool IDEDiskDevice::write_sectors(dword start_sector, word count, const byte* data)
{
    ASSERT(count <= 256);
    LOCKER(m_lock);
#ifdef DISK_DEBUG
    dbgprintf("%s(%u): IDEDiskDevice::write_sectors request (%u sector(s) @ %u)\n",
            current->process().name().characters(),
            current->pid(),
            count,
            start_sector);
#endif
    disable_irq();

    while (IO::in8(IDE0_STATUS) & BUSY);

    //dbgprintf("IDEDiskDevice: Writing %u sector(s) @ LBA %u\n", count, start_sector);

    IO::out8(0x1f2, count == 256 ? 0 : LSB(count));
    IO::out8(0x1f3, start_sector & 0xff);
    IO::out8(0x1f4, (start_sector >> 8) & 0xff);
    IO::out8(0x1f5, (start_sector >> 16) & 0xff);
    IO::out8(0x1f6, 0xe0 | ((start_sector >> 24) & 0xf)); // 0xf0 for 2nd device

    IO::out8(0x3F6, 0x08);

    IO::out8(IDE0_COMMAND, WRITE_SECTORS);

    while (!(IO::in8(IDE0_STATUS) & DRQ));

    byte status = IO::in8(0x1f7);
    ASSERT(status & DRQ);
    IO::repeated_out16(IDE0_DATA, data, count * 256);

    m_interrupted = false;
    enable_irq();
    wait_for_irq();

    disable_irq();
    IO::out8(IDE0_COMMAND, FLUSH_CACHE);
    while (IO::in8(IDE0_STATUS) & BUSY);
    m_interrupted = false;
    enable_irq();
    wait_for_irq();

    return !m_device_error;
}
