#include "CharacterDevice.h"
#include <LibC/errno_numbers.h>

Device::Device(unsigned major, unsigned minor)
     : m_major(major)
     , m_minor(minor)
{
    VFS::the().register_device(*this);
}

Device::~Device()
{
    VFS::the().unregister_device(*this);
}

KResultOr<Retained<FileDescriptor>> Device::open(int options)
{
    UNUSED_PARAM(options);
    return FileDescriptor::create(this);
}

void Device::close()
{
}

int Device::ioctl(Process&, unsigned, unsigned)
{
    return -ENOTTY;
}
