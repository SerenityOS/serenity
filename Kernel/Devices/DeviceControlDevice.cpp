/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceControlDevice.h>
#include <Kernel/Devices/DeviceManagement.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<DeviceControlDevice> DeviceControlDevice::must_create()
{
    auto device_control_device_or_error = DeviceManagement::try_create_device<DeviceControlDevice>();
    // FIXME: Find a way to propagate errors
    VERIFY(!device_control_device_or_error.is_error());
    return device_control_device_or_error.release_value();
}

bool DeviceControlDevice::can_read(const OpenFileDescription&, u64) const
{
    return true;
}

UNMAP_AFTER_INIT DeviceControlDevice::DeviceControlDevice()
    : CharacterDevice(2, 10)
{
}

UNMAP_AFTER_INIT DeviceControlDevice::~DeviceControlDevice()
{
}

ErrorOr<size_t> DeviceControlDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    auto device_event = DeviceManagement::the().dequeue_top_device_event({});
    if (!device_event.has_value())
        return 0;

    if (size < sizeof(DeviceEvent))
        return Error::from_errno(EOVERFLOW);
    size_t nread = 0;
    TRY(buffer.write(&device_event.value(), nread, sizeof(DeviceEvent)));
    nread += sizeof(DeviceEvent);
    return nread;
}

ErrorOr<void> DeviceControlDevice::ioctl(OpenFileDescription&, unsigned, Userspace<void*>)
{
    return Error::from_errno(ENOTSUP);
}

}
