/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Panic.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/ConsoleManagement.h>

namespace Kernel {

static Singleton<ConsoleManagement> s_the;

void ConsoleManagement::resolution_was_changed()
{
    for (auto& console : m_consoles) {
        console.refresh_after_resolution_change();
    }
}

bool ConsoleManagement::is_initialized()
{
    if (!s_the.is_initialized())
        return false;
    if (s_the->m_consoles.is_empty())
        return false;
    if (!s_the->m_active_console)
        return false;
    return true;
}

ConsoleManagement& ConsoleManagement::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT ConsoleManagement::ConsoleManagement()
{
}

UNMAP_AFTER_INIT void ConsoleManagement::initialize()
{
    for (size_t index = 0; index < s_max_virtual_consoles; index++) {
        // FIXME: Better determine the debug TTY we chose...
        if (index == 1) {
            VERIFY(DeviceManagement::the().is_console_device_attached());
            m_consoles.append(VirtualConsole::create_with_preset_log(index, DeviceManagement::the().console_device().logbuffer()));
            continue;
        }
        m_consoles.append(VirtualConsole::create(index));
    }
    // Note: By default the active console is the first one.
    auto tty_number = kernel_command_line().switch_to_tty();
    if (tty_number > m_consoles.size()) {
        PANIC("Switch to tty value is invalid: {} ", tty_number);
    }
    m_active_console = &m_consoles[tty_number];
    SpinlockLocker lock(m_lock);
    m_active_console->set_active(true);
    if (!m_active_console->is_graphical())
        m_active_console->clear();
}

void ConsoleManagement::switch_to(unsigned index)
{
    SpinlockLocker lock(m_lock);
    VERIFY(m_active_console);
    VERIFY(index < m_consoles.size());
    if (m_active_console->index() == index)
        return;

    bool was_graphical = m_active_console->is_graphical();
    m_active_console->set_active(false);
    m_active_console = &m_consoles[index];
    dbgln_if(VIRTUAL_CONSOLE_DEBUG, "Console: Switch to {}", index);

    // Before setting current console to be "active", switch between graphical mode to "textual" mode
    // if needed. This will ensure we clear the screen and also that WindowServer won't print anything
    // in between.
    if (m_active_console->is_graphical() && !was_graphical) {
        GraphicsManagement::the().activate_graphical_mode();
    }
    if (!m_active_console->is_graphical() && was_graphical) {
        GraphicsManagement::the().deactivate_graphical_mode();
    }
    m_active_console->set_active(true);
}

}
