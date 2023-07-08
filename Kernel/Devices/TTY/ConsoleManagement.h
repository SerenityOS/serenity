/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Devices/TTY/VirtualConsole.h>
#include <Kernel/Library/NonnullLockRefPtr.h>

namespace Kernel {

class ConsoleManagement {
    friend class VirtualConsole;

public:
    ConsoleManagement();

    static constexpr size_t s_max_virtual_consoles = 6;

    static bool is_initialized();
    static ConsoleManagement& the();

    void switch_to(unsigned);
    void initialize();

    void resolution_was_changed();

    void switch_to_debug() { switch_to(1); }

    NonnullLockRefPtr<VirtualConsole> first_tty() const { return m_consoles[0]; }
    NonnullLockRefPtr<VirtualConsole> debug_tty() const { return m_consoles[1]; }

    RecursiveSpinlock<LockRank::None>& tty_write_lock() { return m_tty_write_lock; }

private:
    Vector<NonnullLockRefPtr<VirtualConsole>, s_max_virtual_consoles> m_consoles;
    VirtualConsole* m_active_console { nullptr };
    Spinlock<LockRank::None> m_lock {};
    RecursiveSpinlock<LockRank::None> m_tty_write_lock {};
};

};
