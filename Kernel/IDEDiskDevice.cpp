#include "IDEDiskDevice.h"
#include "Disk.h"

RetainPtr<IDEDiskDevice> IDEDiskDevice::create()
{
    return adopt(*new IDEDiskDevice);
}

IDEDiskDevice::IDEDiskDevice()
{
}

IDEDiskDevice::~IDEDiskDevice()
{
}

const char* IDEDiskDevice::className() const
{
    return "IDEDiskDevice";
}

unsigned IDEDiskDevice::blockSize() const
{
    return 512;
}

bool IDEDiskDevice::readBlock(unsigned index, byte* out) const
{
    Disk::readSectors(index, 1, out);
    return true;
}

bool IDEDiskDevice::writeBlock(unsigned index, const byte* data)
{
    (void) index;
    (void) data;
    kprintf("IDEDiskDevice: writeBlock not implemented()\n");
    notImplemented();
    return false;
}

