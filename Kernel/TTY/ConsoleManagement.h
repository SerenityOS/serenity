/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Types.h>
#include <Kernel/TTY/VirtualConsole.h>

namespace Kernel {

class ConsoleManagement {
    AK_MAKE_ETERNAL;
    friend class VirtualConsole;

public:
    ConsoleManagement();

    static constexpr unsigned s_max_virtual_consoles = 6;

    static bool is_initialized();
    static ConsoleManagement& the();

    void switch_to(unsigned);
    void initialize();

    void resolution_was_changed();

    void switch_to_debug() { switch_to(1); }

    NonnullRefPtr<VirtualConsole> first_tty() const { return m_consoles[0]; }
    NonnullRefPtr<VirtualConsole> debug_tty() const { return m_consoles[1]; }

    RecursiveSpinLock& tty_write_lock() { return m_tty_write_lock; }

private:
    NonnullRefPtrVector<VirtualConsole> m_consoles;
    RefPtr<VirtualConsole> m_active_console;
    SpinLock<u8> m_lock;
    RecursiveSpinLock m_tty_write_lock;
};

};
