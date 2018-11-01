#include "types.h"
#include "Process.h"
#include "VGA.h"
#include "Disk.h"
#include "kmalloc.h"
#include "StdLib.h"
#include "IO.h"
#include "i386.h"
#include "PIC.h"

//#define DISK_DEBUG

extern "C" void handle_interrupt();

namespace Disk {

ide_drive_t drive[4];
static volatile bool interrupted;

#define IRQ_FIXED_DISK           14

extern "C" void ide_ISR();

asm(
    ".globl ide_ISR \n"
    "ide_ISR: \n"
    "    pusha\n"
    "    pushw %ds\n"
    "    pushw %es\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    popw %ds\n"
    "    popw %es\n"
    "    call handle_interrupt\n"
    "    popw %es\n"
    "    popw %ds\n"
    "    popa\n"
    "    iret\n"
);

static void enableIRQ()
{
    PIC::enable(IRQ_FIXED_DISK);
}

static void disableIRQ()
{
    PIC::disable(IRQ_FIXED_DISK);
}

static bool waitForInterrupt()
{
#ifdef DISK_DEBUG
    kprintf("disk: waiting for interrupt...\n");
#endif
    // FIXME: Add timeout.
    while (!interrupted) {
        yield();
    }
#ifdef DISK_DEBUG
    kprintf("disk: got interrupt!\n");
#endif
    return true;
}

void interrupt()
{
    IRQHandlerScope scope(IRQ_FIXED_DISK);
#ifdef DISK_DEBUG
    BYTE status = IO::in8(0x1f7);
    kprintf("disk:interrupt: DRQ=%u BUSY=%u DRDY=%u\n", (status & DRQ) != 0, (status & BUSY) != 0, (status & DRDY) != 0);
#endif
    interrupted = true;
}

static SpinLock* s_diskLock;

void initialize()
{
    s_diskLock = new SpinLock;
    disableIRQ();
    interrupted = false;
    registerInterruptHandler(IRQ_VECTOR_BASE + IRQ_FIXED_DISK, ide_ISR);

    while (IO::in8(IDE0_STATUS) & BUSY);

    IO::out8(0x1F6, 0xA0); // 0xB0 for 2nd device
    IO::out8(IDE0_COMMAND, IDENTIFY_DRIVE);

    enableIRQ();
    waitForInterrupt();

    ByteBuffer wbuf = ByteBuffer::createUninitialized(512);
    ByteBuffer bbuf = ByteBuffer::createUninitialized(512);
    BYTE* b = bbuf.pointer();
    WORD* w = (WORD*)wbuf.pointer();
    const WORD* wbufbase = (WORD*)wbuf.pointer();

    for (DWORD i = 0; i < 256; ++i) {
        WORD data = IO::in16(IDE0_DATA);
        *(w++) = data;
        *(b++) = MSB(data);
        *(b++) = LSB(data);
    }

    // "Unpad" the device name string.
    for (DWORD i = 93; i > 54 && bbuf[i] == ' '; --i)
        bbuf[i] = 0;

    drive[0].cylinders = wbufbase[1];
    drive[0].heads = wbufbase[3];
    drive[0].sectors_per_track = wbufbase[6];

    kprintf(
        "ide0: Master=\"%s\", C/H/Spt=%u/%u/%u\n",
        bbuf.pointer() + 54,
        drive[0].cylinders,
        drive[0].heads,
        drive[0].sectors_per_track
    );
}

struct CHS {
    DWORD cylinder;
    WORD head;
    WORD sector;
};

static CHS lba2chs(BYTE drive_index, DWORD lba)
{
    ide_drive_t& d = drive[drive_index];
    CHS chs;
    chs.cylinder = lba / (d.sectors_per_track * d.heads);
    chs.head = (lba / d.sectors_per_track) % d.heads;
    chs.sector = (lba % d.sectors_per_track) + 1;
    return chs;
}

bool readSectors(DWORD startSector, WORD count, BYTE* outbuf)
{
    LOCKER(*s_diskLock);
#ifdef DISK_DEBUG
    kprintf("%s: Disk::readSectors request (%u sector(s) @ %u)\n",
            current->name().characters(),
            count,
            startSector);
#endif
    disableIRQ();

    CHS chs = lba2chs(IDE0_DISK0, startSector);

    while (IO::in8(IDE0_STATUS) & BUSY);

#ifdef DISK_DEBUG
    kprintf("ide0: Reading %u sector(s) @ LBA %u (%u/%u/%u)\n", count, startSector, chs.cylinder, chs.head, chs.sector);
#endif

    IO::out8(0x1F2, count == 256 ? 0 : LSB(count));
    IO::out8(0x1F3, chs.sector);
    IO::out8(0x1F4, LSB(chs.cylinder));
    IO::out8(0x1F5, MSB(chs.cylinder));

    IO::out8(0x1F6, 0xA0 | chs.head); /* 0xB0 for 2nd device */

    IO::out8(0x3F6, 0x08);
    while (!(IO::in8(IDE0_STATUS) & DRDY));

    IO::out8(IDE0_COMMAND, READ_SECTORS);
    interrupted = false;
    enableIRQ();
    waitForInterrupt();

    BYTE status = IO::in8(0x1f7);
    if (status & DRQ) {
#ifdef DISK_DEBUG
        kprintf("Retrieving %u bytes (status=%b), outbuf=%p...\n", count * 512, status, outbuf);
#endif
        for (DWORD i = 0; i < (count * 512); i += 2) {
            WORD w = IO::in16(IDE0_DATA);
            outbuf[i] = LSB(w);
            outbuf[i+1] = MSB(w);
        }
    }

    return true;
}

}

extern "C" void handle_interrupt()
{
    Disk::interrupt();
}
