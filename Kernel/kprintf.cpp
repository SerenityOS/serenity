/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/PrintfImplementation.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/DebugOutput.h>
#if ARCH(I386) || ARCH(X86_64)
#    include <Kernel/Arch/x86/common/BochsDebugOutput.h>
#endif
#include <Kernel/Devices/ConsoleDevice.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/PCISerialDevice.h>
#include <Kernel/Graphics/Console/BootFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/kstdio.h>

#include <LibC/stdarg.h>

namespace Kernel {
extern Atomic<Graphics::Console*> g_boot_console;
}

static bool s_serial_debug_enabled;
// A recursive spinlock allows us to keep writing in the case where a
// page fault happens in the middle of a dbgln(), etc
static RecursiveSpinlock s_log_lock { LockRank::None };

void set_serial_debug_enabled(bool desired_state)
{
    s_serial_debug_enabled = desired_state;
}

bool is_serial_debug_enabled()
{
    return s_serial_debug_enabled;
}

static void serial_putch(char ch)
{
    if (PCISerialDevice::is_available())
        return PCISerialDevice::the().put_char(ch);

    debug_output(ch);
}

static void critical_console_out(char ch)
{
    if (s_serial_debug_enabled)
        serial_putch(ch);

#if ARCH(I386) || ARCH(X86_64)
    // No need to output things to the real ConsoleDevice as no one is likely
    // to read it (because we are in a fatal situation, so only print things and halt)
    bochs_debug_output(ch);
#endif

    // We emit chars directly to the string. this is necessary in few cases,
    // especially when we want to avoid any memory allocations...
    if (GraphicsManagement::is_initialized() && GraphicsManagement::the().console()) {
        GraphicsManagement::the().console()->write(ch, true);
    } else if (auto* boot_console = g_boot_console.load()) {
        boot_console->write(ch, true);
    }
}

static void console_out(char ch)
{
    if (s_serial_debug_enabled)
        serial_putch(ch);

    // It would be bad to reach the assert in ConsoleDevice()::the() and do a stack overflow

    if (DeviceManagement::the().is_console_device_attached()) {
        DeviceManagement::the().console_device().put_char(ch);
    } else {
#if ARCH(I386) || ARCH(X86_64)
        bochs_debug_output(ch);
#endif
    }
    if (ConsoleManagement::is_initialized()) {
        ConsoleManagement::the().debug_tty()->emit_char(ch);
    } else if (auto* boot_console = g_boot_console.load()) {
        boot_console->write(ch, true);
    }
}

static void buffer_putch(char*& bufptr, char ch)
{
    *bufptr++ = ch;
}

// Declare it, so that the symbol is exported, because libstdc++ uses it.
// However, *only* libstdc++ uses it, and none of the rest of the Kernel.
extern "C" int sprintf(char* buffer, char const* fmt, ...);

int sprintf(char* buffer, char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal(buffer_putch, buffer, fmt, ap);
    buffer[ret] = '\0';
    va_end(ap);
    return ret;
}

int snprintf(char* buffer, size_t size, char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    size_t space_remaining = 0;
    if (size) {
        space_remaining = size - 1;
    } else {
        space_remaining = 0;
    }
    auto sized_buffer_putch = [&](char*& bufptr, char ch) {
        if (space_remaining) {
            *bufptr++ = ch;
            --space_remaining;
        }
    };
    int ret = printf_internal(sized_buffer_putch, buffer, fmt, ap);
    if (space_remaining) {
        buffer[ret] = '\0';
    } else if (size > 0) {
        buffer[size - 1] = '\0';
    }
    va_end(ap);
    return ret;
}

static inline void internal_dbgputch(char ch)
{
    if (s_serial_debug_enabled)
        serial_putch(ch);
#if ARCH(I386) || ARCH(X86_64)
    bochs_debug_output(ch);
#endif
}

extern "C" void dbgputstr(char const* characters, size_t length)
{
    if (!characters)
        return;
    SpinlockLocker lock(s_log_lock);
    for (size_t i = 0; i < length; ++i)
        internal_dbgputch(characters[i]);
}

void dbgputstr(StringView view)
{
    ::dbgputstr(view.characters_without_null_termination(), view.length());
}

extern "C" void kernelputstr(char const* characters, size_t length)
{
    if (!characters)
        return;
    SpinlockLocker lock(s_log_lock);
    for (size_t i = 0; i < length; ++i)
        console_out(characters[i]);
}

extern "C" void kernelcriticalputstr(char const* characters, size_t length)
{
    if (!characters)
        return;
    SpinlockLocker lock(s_log_lock);
    for (size_t i = 0; i < length; ++i)
        critical_console_out(characters[i]);
}

extern "C" void kernelearlyputstr(char const* characters, size_t length)
{
    if (!characters)
        return;
    // NOTE: We do not lock the log lock here, as this function is called before this or any other processor was initialized, meaning:
    //  A) The $gs base was not setup yet, so we cannot enter into critical sections, and as a result we cannot use SpinLocks
    //  B) No other processors may try to print at the same time anyway
    for (size_t i = 0; i < length; ++i)
        internal_dbgputch(characters[i]);
}
