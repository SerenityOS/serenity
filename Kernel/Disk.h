#pragma once

#include "types.h"
#include "DataBuffer.h"

#define IDE0_DATA        0x1F0
#define IDE0_STATUS      0x1F7
#define IDE0_COMMAND     0x1F7
#define BUSY             0x80
#define DRDY             0x40
#define DRQ              0x08
#define IDENTIFY_DRIVE   0xEC
#define READ_SECTORS     0x21

#define IDE0_DISK0       0
#define IDE0_DISK1       1
#define IDE1_DISK0       2
#define IDE1_DISK1       3

typedef struct
{
    WORD cylinders;
    WORD heads;
    WORD sectors_per_track;
} ide_drive_t;

extern void ide_init();
extern ide_drive_t drive[4];

namespace Disk {

void initialize();
bool readSectors(DWORD sectorIndex, WORD count, BYTE* buffer);

}
