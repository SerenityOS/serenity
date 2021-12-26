#include "NullDevice.h"
#include <AK/StdLibExtras.h>
#include <AK/kstdio.h>

static NullDevice* s_the;

NullDevice& NullDevice::the()
{
    ASSERT(s_the);
    return *s_the;
}

NullDevice::NullDevice()
    : CharacterDevice(1, 3)
{
    s_the = this;
}

NullDevice::~NullDevice()
{
}

bool NullDevice::can_read(FileDescriptor&) const
{
    return true;
}

ssize_t NullDevice::read(FileDescriptor&, byte*, ssize_t)
{
    return 0;
}

ssize_t NullDevice::write(FileDescriptor&, const byte*, ssize_t buffer_size)
{
    return min(PAGE_SIZE, buffer_size);
}

