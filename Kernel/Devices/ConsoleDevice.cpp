/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Devices/ConsoleDevice.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Sections.h>
#include <Kernel/kstdio.h>

// Output bytes to kernel debug port 0xE9 (Bochs console). It's very handy.
#define CONSOLE_OUT_TO_BOCHS_DEBUG_PORT

static Kernel::Spinlock g_console_lock;

UNMAP_AFTER_INIT NonnullRefPtr<ConsoleDevice> ConsoleDevice::must_create()
{
    auto device_or_error = DeviceManagement::try_create_device<ConsoleDevice>();
    VERIFY(!device_or_error.is_error());
    return device_or_error.release_value();
}

UNMAP_AFTER_INIT ConsoleDevice::ConsoleDevice()
    : CharacterDevice(5, 1)
{
}

UNMAP_AFTER_INIT ConsoleDevice::~ConsoleDevice()
{
}

bool ConsoleDevice::can_read(const Kernel::OpenFileDescription&, size_t) const
{
    return false;
}

ErrorOr<size_t> ConsoleDevice::read(OpenFileDescription&, u64, Kernel::UserOrKernelBuffer&, size_t)
{
    // FIXME: Implement reading from the console.
    //        Maybe we could use a ring buffer for this device?
    return 0;
}

ErrorOr<size_t> ConsoleDevice::write(OpenFileDescription&, u64, const Kernel::UserOrKernelBuffer& data, size_t size)
{
    if (!size)
        return 0;

    return data.read_buffered<256>(size, [&](ReadonlyBytes readonly_bytes) {
        for (const auto& byte : readonly_bytes)
            put_char(byte);
        return readonly_bytes.size();
    });
}

void ConsoleDevice::put_char(char ch)
{
    Kernel::SpinlockLocker lock(g_console_lock);
#ifdef CONSOLE_OUT_TO_BOCHS_DEBUG_PORT
    IO::out8(IO::BOCHS_DEBUG_PORT, ch);
#endif
    m_logbuffer.enqueue(ch);
}
