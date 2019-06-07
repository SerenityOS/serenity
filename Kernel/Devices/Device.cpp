#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <LibC/errno_numbers.h>

Device::Device(unsigned major, unsigned minor)
     : m_major(major)
     , m_minor(minor)
{
    VFS::the().register_device({}, *this);
}

Device::~Device()
{
    VFS::the().unregister_device({}, *this);
}

String Device::absolute_path(const FileDescription&) const
{
    return String::format("device:%u,%u (%s)", m_major, m_minor, class_name());
}
