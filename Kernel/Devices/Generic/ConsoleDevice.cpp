/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/BochsDebugOutput.h>
#endif
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Generic/ConsoleDevice.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Sections.h>
#include <Kernel/kstdio.h>

namespace Kernel {

Spinlock<LockRank::None> g_console_lock {};

UNMAP_AFTER_INIT NonnullRefPtr<ConsoleDevice> ConsoleDevice::must_create()
{
    return MUST(Device::try_create_device<ConsoleDevice>());
}

UNMAP_AFTER_INIT ConsoleDevice::ConsoleDevice()
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::Console, 1)
{
}

UNMAP_AFTER_INIT ConsoleDevice::~ConsoleDevice() = default;

bool ConsoleDevice::can_read(Kernel::OpenFileDescription const&, u64) const
{
    return false;
}

ErrorOr<size_t> ConsoleDevice::read(OpenFileDescription&, u64, Kernel::UserOrKernelBuffer&, size_t)
{
    // FIXME: Implement reading from the console.
    //        Maybe we could use a ring buffer for this device?
    return 0;
}

ErrorOr<size_t> ConsoleDevice::write(OpenFileDescription&, u64, Kernel::UserOrKernelBuffer const& data, size_t size)
{
    if (!size)
        return 0;

    return data.read_buffered<256>(size, [&](ReadonlyBytes readonly_bytes) {
        for (auto const& byte : readonly_bytes)
            put_char(byte);
        return readonly_bytes.size();
    });
}

void ConsoleDevice::put_char(char ch)
{
    Kernel::SpinlockLocker lock(g_console_lock);
    dbgputchar(ch);
    m_logbuffer.enqueue(ch);
}

}
