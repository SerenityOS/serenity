/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/NonnullOwnPtr.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/KCOVDevice.h>
#include <Kernel/Devices/KCOVInstance.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <LibC/sys/ioctl_numbers.h>

#include <Kernel/Panic.h>

namespace Kernel {

HashMap<ProcessID, KCOVInstance*>* KCOVDevice::proc_instance;
HashMap<ThreadID, KCOVInstance*>* KCOVDevice::thread_instance;

UNMAP_AFTER_INIT NonnullRefPtr<KCOVDevice> KCOVDevice::must_create()
{
    auto kcov_device_or_error = DeviceManagement::try_create_device<KCOVDevice>();
    // FIXME: Find a way to propagate errors
    VERIFY(!kcov_device_or_error.is_error());
    return kcov_device_or_error.release_value();
}

UNMAP_AFTER_INIT KCOVDevice::KCOVDevice()
    : BlockDevice(30, 0)
{
    proc_instance = new HashMap<ProcessID, KCOVInstance*>();
    thread_instance = new HashMap<ThreadID, KCOVInstance*>();
    dbgln("KCOVDevice created");
}

void KCOVDevice::free_thread()
{
    auto thread = Thread::current();
    auto tid = thread->tid();

    auto maybe_kcov_instance = thread_instance->get(tid);
    if (!maybe_kcov_instance.has_value())
        return;

    auto kcov_instance = maybe_kcov_instance.value();
    VERIFY(kcov_instance->state() == KCOVInstance::TRACING);
    kcov_instance->set_state(KCOVInstance::OPENED);
    thread_instance->remove(tid);
}

void KCOVDevice::free_process()
{
    auto pid = Process::current().pid();

    auto maybe_kcov_instance = proc_instance->get(pid);
    if (!maybe_kcov_instance.has_value())
        return;

    auto kcov_instance = maybe_kcov_instance.value();
    VERIFY(kcov_instance->state() == KCOVInstance::OPENED);
    kcov_instance->set_state(KCOVInstance::UNUSED);
    proc_instance->remove(pid);
    delete kcov_instance;
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> KCOVDevice::open(int options)
{
    auto pid = Process::current().pid();
    if (proc_instance->get(pid).has_value())
        return EBUSY; // This process already open()ed the kcov device
    auto kcov_instance = new KCOVInstance(pid);
    kcov_instance->set_state(KCOVInstance::OPENED);
    proc_instance->set(pid, kcov_instance);

    return File::open(options);
}

ErrorOr<void> KCOVDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    auto thread = Thread::current();
    auto tid = thread->tid();
    auto pid = thread->pid();
    auto maybe_kcov_instance = proc_instance->get(pid);
    if (!maybe_kcov_instance.has_value())
        return ENXIO; // This proc hasn't opened the kcov dev yet
    auto kcov_instance = maybe_kcov_instance.value();

    SpinlockLocker locker(kcov_instance->spinlock());
    switch (request) {
    case KCOV_SETBUFSIZE:
        if (kcov_instance->state() >= KCOVInstance::TRACING)
            return EBUSY;
        return kcov_instance->buffer_allocate((FlatPtr)arg.unsafe_userspace_ptr());
    case KCOV_ENABLE:
        if (kcov_instance->state() >= KCOVInstance::TRACING)
            return EBUSY;
        if (!kcov_instance->has_buffer())
            return ENOBUFS;
        VERIFY(kcov_instance->state() == KCOVInstance::OPENED);
        kcov_instance->set_state(KCOVInstance::TRACING);
        thread_instance->set(tid, kcov_instance);
        return {};
    case KCOV_DISABLE: {
        auto maybe_kcov_instance = thread_instance->get(tid);
        if (!maybe_kcov_instance.has_value())
            return ENOENT;
        VERIFY(kcov_instance->state() == KCOVInstance::TRACING);
        kcov_instance->set_state(KCOVInstance::OPENED);
        thread_instance->remove(tid);
        return {};
    }
    default:
        return EINVAL;
    }
}

ErrorOr<Memory::Region*> KCOVDevice::mmap(Process& process, OpenFileDescription&, Memory::VirtualRange const& range, u64 offset, int prot, bool shared)
{
    auto pid = process.pid();
    auto maybe_kcov_instance = proc_instance->get(pid);
    VERIFY(maybe_kcov_instance.has_value()); // Should happen on fd open()
    auto kcov_instance = maybe_kcov_instance.value();

    if (!kcov_instance->vmobject())
        return ENOBUFS; // mmaped, before KCOV_SETBUFSIZE

    return process.address_space().allocate_region_with_vmobject(
        range, *kcov_instance->vmobject(), offset, {}, prot, shared);
}

}
