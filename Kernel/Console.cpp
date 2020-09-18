/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

void Console::initialize()
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

Console::Console()
    : CharacterDevice(5, 1)
{
}

Console::~Console()
{
}

bool Console::can_read(const Kernel::FileDescription&, size_t) const
{
    return false;
}

Kernel::KResultOr<size_t> Console::read(Kernel::FileDescription&, size_t, Kernel::UserOrKernelBuffer&, size_t)
{
    // FIXME: Implement reading from the console.
    //        Maybe we could use a ring buffer for this device?
    return 0;
}

Kernel::KResultOr<size_t> Console::write(Kernel::FileDescription&, size_t, const Kernel::UserOrKernelBuffer& data, size_t size)
{
    if (!size)
        return 0;

    ssize_t nread = data.read_buffered<256>(size, [&](const u8* bytes, size_t bytes_count) {
        for (size_t i = 0; i < bytes_count; i++)
            put_char((char)bytes[i]);
        return (ssize_t)bytes_count;
    });
    if (nread < 0)
        return Kernel::KResult(nread);
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
