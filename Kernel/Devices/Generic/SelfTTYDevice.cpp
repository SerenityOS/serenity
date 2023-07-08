/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/Generic/SelfTTYDevice.h>
#include <Kernel/Devices/TTY/TTY.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<SelfTTYDevice> SelfTTYDevice::must_create()
{
    auto self_tty_device_or_error = DeviceManagement::try_create_device<SelfTTYDevice>();
    // FIXME: Find a way to propagate errors
    VERIFY(!self_tty_device_or_error.is_error());
    return self_tty_device_or_error.release_value();
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> SelfTTYDevice::open(int options)
{
    // Note: If for some odd reason we try to open this device (early on boot?)
    // while there's no current Process assigned, don't fail and return an error.
    if (!Process::has_current())
        return Error::from_errno(ESRCH);
    auto& current_process = Process::current();
    auto tty = current_process.tty();
    if (!tty)
        return Error::from_errno(ENXIO);
    auto description = TRY(OpenFileDescription::try_create(*tty));
    description->set_rw_mode(options);
    description->set_file_flags(options);
    return description;
}

bool SelfTTYDevice::can_read(OpenFileDescription const&, u64) const
{
    VERIFY_NOT_REACHED();
}

bool SelfTTYDevice::can_write(OpenFileDescription const&, u64) const
{
    VERIFY_NOT_REACHED();
}

ErrorOr<size_t> SelfTTYDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    VERIFY_NOT_REACHED();
}

ErrorOr<size_t> SelfTTYDevice::write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t)
{
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT SelfTTYDevice::SelfTTYDevice()
    : CharacterDevice(5, 0)
{
}

UNMAP_AFTER_INIT SelfTTYDevice::~SelfTTYDevice()
{
}

}
