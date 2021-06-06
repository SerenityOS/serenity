/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/NonnullOwnPtr.h>
#include <Kernel/Devices/KCOVDevice.h>
#include <Kernel/Devices/KCOVInstance.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <LibC/sys/ioctl_numbers.h>

#include <Kernel/Panic.h>

namespace Kernel {

HashMap<ProcessID, KCOVInstance*>* KCOVDevice::proc_instance;
HashMap<ThreadID, KCOVInstance*>* KCOVDevice::thread_instance;

UNMAP_AFTER_INIT NonnullRefPtr<KCOVDevice> KCOVDevice::must_create()
{
    return adopt_ref(*new KCOVDevice);
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
    VERIFY(kcov_instance->state == KCOVInstance::TRACING);
    kcov_instance->state = KCOVInstance::OPENED;
    thread_instance->remove(tid);
}

void KCOVDevice::free_process()
{
    auto process = Process::current();
    auto pid = process->pid();

    auto maybe_kcov_instance = proc_instance->get(pid);
    if (!maybe_kcov_instance.has_value())
        return;

    auto kcov_instance = maybe_kcov_instance.value();
    VERIFY(kcov_instance->state == KCOVInstance::OPENED);
    kcov_instance->state = KCOVInstance::UNUSED;
    proc_instance->remove(pid);
    delete kcov_instance;
}

KResultOr<NonnullRefPtr<FileDescription>> KCOVDevice::open(int options)
{
    auto process = Process::current();
    auto pid = process->pid();
    if (proc_instance->get(pid).has_value())
        return EBUSY; // This process already open()ed the kcov device
    auto kcov_instance = new KCOVInstance(pid);
    kcov_instance->state = KCOVInstance::OPENED;
    proc_instance->set(pid, kcov_instance);

    return File::open(options);
}

int KCOVDevice::ioctl(FileDescription&, unsigned request, FlatPtr arg)
{
    int error = 0;
    auto thread = Thread::current();
    auto tid = thread->tid();
    auto pid = thread->pid();
    auto maybe_kcov_instance = proc_instance->get(pid);
    if (!maybe_kcov_instance.has_value())
        return ENXIO; // This proc hasn't opened the kcov dev yet
    auto kcov_instance = maybe_kcov_instance.value();

    ScopedSpinLock lock(kcov_instance->lock);
    switch (request) {
    case KCOV_SETBUFSIZE: {
        if (kcov_instance->state >= KCOVInstance::TRACING) {
            error = EBUSY;
            break;
        }
        error = kcov_instance->buffer_allocate(arg);
        break;
    }
    case KCOV_ENABLE: {
        if (kcov_instance->state >= KCOVInstance::TRACING) {
            error = EBUSY;
            break;
        }
        if (!kcov_instance->has_buffer()) {
            error = ENOBUFS;
            break;
        }
        VERIFY(kcov_instance->state == KCOVInstance::OPENED);
        kcov_instance->state = KCOVInstance::TRACING;
        thread_instance->set(tid, kcov_instance);
        break;
    }
    case KCOV_DISABLE: {
        auto maybe_kcov_instance = thread_instance->get(tid);
        if (!maybe_kcov_instance.has_value()) {
            error = ENOENT;
            break;
        }
        VERIFY(kcov_instance->state == KCOVInstance::TRACING);
        kcov_instance->state = KCOVInstance::OPENED;
        thread_instance->remove(tid);
        break;
    }
    default: {
        error = EINVAL;
    }
    };

    return error;
}

KResultOr<Region*> KCOVDevice::mmap(Process& process, FileDescription&, const Range& range, u64 offset, int prot, bool shared)
{
    auto pid = process.pid();
    auto maybe_kcov_instance = proc_instance->get(pid);
    VERIFY(maybe_kcov_instance.has_value()); // Should happen on fd open()
    auto kcov_instance = maybe_kcov_instance.value();

    if (!kcov_instance->vmobject) {
        return ENOBUFS; // Mmaped, before KCOV_SETBUFSIZE
    }

    return process.space().allocate_region_with_vmobject(
        range, *kcov_instance->vmobject, offset, {}, prot, shared);
}

String KCOVDevice::device_name() const
{
    return "kcov"sv;
}

}
