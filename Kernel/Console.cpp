/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Console.h>
#include <Kernel/IO.h>
#include <Kernel/SpinLock.h>
#include <Kernel/kstdio.h>

// Bytes output to 0xE9 end up on the Bochs console. It's very handy.
#define CONSOLE_OUT_TO_E9

static AK::Singleton<Console> s_the;
static Kernel::SpinLock g_console_lock;

UNMAP_AFTER_INIT void Console::initialize()
{
    s_the.ensure_instance();
}

Console& Console::the()
{
    return *s_the;
}

bool Console::is_initialized()
{
    return s_the.is_initialized();
}

UNMAP_AFTER_INIT Console::Console()
    : CharacterDevice(5, 1)
{
}

UNMAP_AFTER_INIT Console::~Console()
{
}

bool Console::can_read(const Kernel::FileDescription&, size_t) const
{
    return false;
}

Kernel::KResultOr<size_t> Console::read(FileDescription&, u64, Kernel::UserOrKernelBuffer&, size_t)
{
    // FIXME: Implement reading from the console.
    //        Maybe we could use a ring buffer for this device?
    return 0;
}

Kernel::KResultOr<size_t> Console::write(FileDescription&, u64, const Kernel::UserOrKernelBuffer& data, size_t size)
{
    if (!size)
        return 0;

    ssize_t nread = data.read_buffered<256>(size, [&](const u8* bytes, size_t bytes_count) {
        for (size_t i = 0; i < bytes_count; i++)
            put_char((char)bytes[i]);
        return (ssize_t)bytes_count;
    });
    if (nread < 0)
        return Kernel::KResult((ErrnoCode)-nread);
    return (size_t)nread;
}

void Console::put_char(char ch)
{
    Kernel::ScopedSpinLock lock(g_console_lock);
#ifdef CONSOLE_OUT_TO_E9
    //if (ch != 27)
    IO::out8(0xe9, ch);
#endif
    m_logbuffer.enqueue(ch);
}
