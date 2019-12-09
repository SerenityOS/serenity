#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <LibC/errno_numbers.h>

static HashMap<u32, Device*>* s_all_devices;

HashMap<u32, Device*>& Device::all_devices()
{
    if (s_all_devices == nullptr)
        s_all_devices = new HashMap<u32, Device*>;
    return *s_all_devices;
}

void Device::for_each(Function<void(Device&)> callback)
{
    for (auto& entry : all_devices())
        callback(*entry.value);
}

Device* Device::get_device(unsigned major, unsigned minor)
{
    auto it = all_devices().find(encoded_device(major, minor));
    if (it == all_devices().end())
        return nullptr;
    return it->value;
}

Device::Device(unsigned major, unsigned minor)
    : m_major(major)
    , m_minor(minor)
{
    u32 device_id = encoded_device(major, minor);
    auto it = all_devices().find(device_id);
    if (it != all_devices().end()) {
        dbg() << "Already registered " << major << "," << minor << ": " << it->value->class_name();
    }
    ASSERT(!all_devices().contains(device_id));
    all_devices().set(device_id, this);
}

Device::~Device()
{
    all_devices().remove(encoded_device(m_major, m_minor));
}

String Device::absolute_path() const
{
    return String::format("device:%u,%u (%s)", m_major, m_minor, class_name());
}

String Device::absolute_path(const FileDescription&) const
{
    return absolute_path();
}
