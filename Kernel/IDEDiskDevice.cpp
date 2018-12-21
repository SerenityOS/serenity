#include "IDEDiskDevice.h"
#include "types.h"
#include "Process.h"
#include "StdLib.h"
#include "IO.h"
#include "Scheduler.h"
#include "PIC.h"
#include <AK/Lock.h>

//#define DISK_DEBUG

#define IRQ_FIXED_DISK 14

#define IDE0_DATA        0x1F0
#define IDE0_STATUS      0x1F7
#define IDE0_COMMAND     0x1F7

enum IDECommand : byte {
    IDENTIFY_DRIVE = 0xEC,
    READ_SECTORS = 0x21,
    WRITE_SECTORS = 0x30,
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

RetainPtr<IDEDiskDevice> IDEDiskDevice::create()
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
    const_cast<IDEDiskDevice&>(*this).read_sectors(index, 1, out);
    return true;
}

bool IDEDiskDevice::write_block(unsigned index, const byte* data)
{
    write_sectors(index, 1, data);
    return true;
}

#ifdef DISK_DEBUG
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
#endif

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
    return true;
}

void IDEDiskDevice::handle_irq()
{
#ifdef DISK_DEBUG
    byte status = IO::in8(0x1f7);
    kprintf("disk:interrupt: DRQ=%u BUSY=%u DRDY=%u\n", (status & DRQ) != 0, (status & BUSY) != 0, (status & DRDY) != 0);
#endif
    m_interrupted = true;
}

void IDEDiskDevice::initialize()
{
    byte status;

    status = IO::in8(IDE0_STATUS);
#ifdef DISK_DEBUG
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
        "ide0: Master=\"%s\", C/H/Spt=%u/%u/%u\n",
        bbuf.pointer() + 54,
        m_cylinders,
        m_heads,
        m_sectors_per_track
    );
}

IDEDiskDevice::CHS IDEDiskDevice::lba_to_chs(dword lba) const
{
    CHS chs;
    chs.cylinder = lba / (m_sectors_per_track * m_heads);
    chs.head = (lba / m_sectors_per_track) % m_heads;
    chs.sector = (lba % m_sectors_per_track) + 1;
    return chs;
}

bool IDEDiskDevice::read_sectors(dword start_sector, word count, byte* outbuf)
{
    LOCKER(m_lock);
#ifdef DISK_DEBUG
    kprintf("%s: Disk::read_sectors request (%u sector(s) @ %u)\n",
            current->name().characters(),
            count,
            start_sector);
#endif
    disable_irq();

    auto chs = lba_to_chs(start_sector);

    while (IO::in8(IDE0_STATUS) & BUSY);

#ifdef DISK_DEBUG
    kprintf("ide0: Reading %u sector(s) @ LBA %u (%u/%u/%u)\n", count, start_sector, chs.cylinder, chs.head, chs.sector);
#endif

    IO::out8(0x1F2, count == 256 ? 0 : LSB(count));
    IO::out8(0x1F3, chs.sector);
    IO::out8(0x1F4, LSB(chs.cylinder));
    IO::out8(0x1F5, MSB(chs.cylinder));

    IO::out8(0x1F6, 0xA0 | chs.head); /* 0xB0 for 2nd device */

    IO::out8(0x3F6, 0x08);
    while (!(IO::in8(IDE0_STATUS) & DRDY));

    IO::out8(IDE0_COMMAND, READ_SECTORS);
    m_interrupted = false;
    enable_irq();
    wait_for_irq();

    byte status = IO::in8(0x1f7);
    if (status & DRQ) {
#ifdef DISK_DEBUG
        kprintf("Retrieving %u bytes (status=%b), outbuf=%p...\n", count * 512, status, outbuf);
#endif
        for (dword i = 0; i < (count * 512); i += 2) {
            word w = IO::in16(IDE0_DATA);
            outbuf[i] = LSB(w);
            outbuf[i+1] = MSB(w);
        }
    }

    return true;
}

bool IDEDiskDevice::write_sectors(dword start_sector, word count, const byte* data)
{
    LOCKER(m_lock);
    dbgprintf("%s(%u): IDEDiskDevice::write_sectors request (%u sector(s) @ %u)\n",
            current->name().characters(),
            current->pid(),
            count,
            start_sector);
    disable_irq();

    auto chs = lba_to_chs(start_sector);

    while (IO::in8(IDE0_STATUS) & BUSY);

    //dbgprintf("IDEDiskDevice: Writing %u sector(s) @ LBA %u (%u/%u/%u)\n", count, start_sector, chs.cylinder, chs.head, chs.sector);

    IO::out8(0x1F2, count == 256 ? 0 : LSB(count));
    IO::out8(0x1F3, chs.sector);
    IO::out8(0x1F4, LSB(chs.cylinder));
    IO::out8(0x1F5, MSB(chs.cylinder));

    IO::out8(0x1F6, 0xA0 | chs.head); /* 0xB0 for 2nd device */

    IO::out8(0x3F6, 0x08);

    IO::out8(IDE0_COMMAND, WRITE_SECTORS);

    while (!(IO::in8(IDE0_STATUS) & DRQ));

    byte status = IO::in8(0x1f7);
    if (status & DRQ) {
        //dbgprintf("Sending %u bytes (status=%b), data=%p...\n", count * 512, status, data);
        auto* data_as_words = (const word*)data;
        for (dword i = 0; i < (count * 512) / 2; ++i) {
            IO::out16(IDE0_DATA, data_as_words[i]);
        }
    }

    m_interrupted = false;
    enable_irq();
    wait_for_irq();

    return true;
}
