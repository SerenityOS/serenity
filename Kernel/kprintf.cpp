/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/PrintfImplementation.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/DebugOutput.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/BochsDebugOutput.h>
#endif
#include <Kernel/Devices/BaseDevices.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/GPU/Console/BootFramebufferConsole.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Devices/Generic/ConsoleDevice.h>
#include <Kernel/Devices/PCISerialDevice.h>
#include <Kernel/Devices/TTY/VirtualConsole.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/kstdio.h>

namespace Kernel {
extern Atomic<Graphics::Console*> g_boot_console;
}

static bool s_serial_debug_enabled;
// A recursive spinlock allows us to keep writing in the case where a
// page fault happens in the middle of a dbgln(), etc
static RecursiveSpinlock<LockRank::None> s_log_lock {};

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

#if ARCH(X86_64)
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
    auto base_devices = Device::base_devices();
    if (base_devices) {
        base_devices->console_device->put_char(ch);
    } else {
        if (s_serial_debug_enabled)
            serial_putch(ch);

#if ARCH(X86_64)
        bochs_debug_output(ch);
#endif
    }
    if (!VirtualConsole::emit_char_on_debug_console(ch)) {
        if (auto* boot_console = g_boot_console.load())
            boot_console->write(ch, true);
    }
}

// Declare it, so that the symbol is exported, because libstdc++ uses it.
// However, *only* libstdc++ uses it, and none of the rest of the Kernel.
extern "C" int sprintf(char* buffer, char const* fmt, ...);

int sprintf(char*, char const*, ...)
{
    VERIFY_NOT_REACHED();
}

static inline void internal_dbgputch(char ch)
{
    if (s_serial_debug_enabled)
        serial_putch(ch);
#if ARCH(X86_64)
    bochs_debug_output(ch);
#endif
}

extern "C" void dbgputchar(char ch)
{
    internal_dbgputch(ch);
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
