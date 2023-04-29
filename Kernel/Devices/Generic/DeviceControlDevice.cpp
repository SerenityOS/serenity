/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/Generic/DeviceControlDevice.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<DeviceControlDevice> DeviceControlDevice::must_create()
{
    auto device_control_device_or_error = DeviceManagement::try_create_device<DeviceControlDevice>();
    // FIXME: Find a way to propagate errors
    VERIFY(!device_control_device_or_error.is_error());
    return device_control_device_or_error.release_value();
}

bool DeviceControlDevice::can_read(OpenFileDescription const&, u64) const
{
    return DeviceManagement::the().event_queue({}).with([](auto& queue) -> bool {
        return !queue.is_empty();
    });
}

UNMAP_AFTER_INIT DeviceControlDevice::DeviceControlDevice()
    : CharacterDevice(2, 10)
{
}

UNMAP_AFTER_INIT DeviceControlDevice::~DeviceControlDevice() = default;

ErrorOr<size_t> DeviceControlDevice::read(OpenFileDescription&, u64 offset, UserOrKernelBuffer& buffer, size_t size)
{
    if (offset != 0)
        return Error::from_errno(EINVAL);
    if ((size % sizeof(DeviceEvent)) != 0)
        return Error::from_errno(EOVERFLOW);

    return DeviceManagement::the().event_queue({}).with([&](auto& queue) -> ErrorOr<size_t> {
        size_t nread = 0;
        for (size_t event_index = 0; event_index < (size / sizeof(DeviceEvent)); event_index++) {
            if (queue.is_empty())
                break;
            auto event = queue.dequeue();
            TRY(buffer.write(&event, nread, sizeof(DeviceEvent)));
            nread += sizeof(DeviceEvent);
        }
        return nread;
    });
}

ErrorOr<void> DeviceControlDevice::ioctl(OpenFileDescription&, unsigned, Userspace<void*>)
{
    return Error::from_errno(ENOTSUP);
}

}
