#include <Kernel/Devices/RandomDevice.h>
#include <Kernel/Random.h>

RandomDevice::RandomDevice()
    : CharacterDevice(1, 8)
{
}

RandomDevice::~RandomDevice()
{
}

bool RandomDevice::can_read(const FileDescription&) const
{
    return true;
}

ssize_t RandomDevice::read(FileDescription&, u8* buffer, ssize_t size)
{
    get_good_random_bytes(buffer, size);
    return size;
}

ssize_t RandomDevice::write(FileDescription&, const u8*, ssize_t size)
{
    // FIXME: Use input for entropy? I guess that could be a neat feature?
    return min(PAGE_SIZE, size);
}
