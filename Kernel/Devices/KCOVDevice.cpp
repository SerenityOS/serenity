/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/NonnullOwnPtr.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/KCOVDevice.h>
#include <Kernel/Devices/KCOVInstance.h>
#include <Kernel/FileSystem/OpenFileDescription.h>

#include <Kernel/Library/Panic.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<KCOVDevice> KCOVDevice::must_create()
{
    auto kcov_device_or_error = DeviceManagement::try_create_device<KCOVDevice>();
    // FIXME: Find a way to propagate errors
    VERIFY(!kcov_device_or_error.is_error());
    return kcov_device_or_error.release_value();
}

UNMAP_AFTER_INIT KCOVDevice::KCOVDevice()
    : BlockDevice(30, 0)
{
    dbgln("KCOVDevice created");
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> KCOVDevice::open(int options)
{
    auto& proc = Process::current();
    auto* kcov_instance = proc.kcov_instance();
    if (kcov_instance != nullptr)
        return EBUSY; // This process already open()ed the kcov device

    proc.set_kcov_instance(new KCOVInstance(proc.pid()));
    return Device::open(options);
}

ErrorOr<void> KCOVDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    auto& proc = Process::current();
    auto* thread = Thread::current();
    auto* kcov_instance = proc.kcov_instance();
    if (kcov_instance == nullptr) {
        VERIFY(!Process::is_kcov_busy()); // No thread should be tracing at this point.
        return ENXIO;                     // This proc hasn't opened the kcov dev yet
    }

    SpinlockLocker locker(kcov_instance->spinlock());
    switch (request) {
    case KCOV_SETBUFSIZE:
        if (Process::is_kcov_busy())
            // Buffer is shared among all proc threads, hence we need to check if any of them
            // are currently tracing. If so, don't mess with the buffer.
            return EBUSY;
        return kcov_instance->buffer_allocate((FlatPtr)arg.unsafe_userspace_ptr());
    case KCOV_ENABLE:
        if (!kcov_instance->has_buffer())
            return ENOBUFS;
        thread->m_kcov_enabled = true;
        return {};
    case KCOV_DISABLE: {
        thread->m_kcov_enabled = false;
        return {};
    }
    default:
        return EINVAL;
    }
}

ErrorOr<NonnullLockRefPtr<Memory::VMObject>> KCOVDevice::vmobject_for_mmap(Process& process, Memory::VirtualRange const&, u64&, bool)
{
    auto* kcov_instance = process.kcov_instance();
    VERIFY(kcov_instance != nullptr); // Should have happened on fd open()

    if (!kcov_instance->vmobject())
        return ENOBUFS; // mmaped, before KCOV_SETBUFSIZE

    return *kcov_instance->vmobject();
}

}
