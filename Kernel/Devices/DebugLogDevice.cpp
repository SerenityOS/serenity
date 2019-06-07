#include <Kernel/Devices/DebugLogDevice.h>
#include <Kernel/IO.h>

static DebugLogDevice* s_the;

DebugLogDevice& DebugLogDevice::the()
{
    ASSERT(s_the);
    return *s_the;
}

DebugLogDevice::DebugLogDevice()
    : CharacterDevice(1, 18)
{
    s_the = this;
}

DebugLogDevice::~DebugLogDevice()
{
}

ssize_t DebugLogDevice::write(FileDescription&, const byte* data, ssize_t data_size)
{
    for (int i = 0; i < data_size; ++i)
        IO::out8(0xe9, data[i]);
    return data_size;
}
