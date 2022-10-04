/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Platform.h>
#include <AK/Types.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel {

class GraphicsManagement;
class VGAIOArbiter {
public:
    static NonnullOwnPtr<VGAIOArbiter> must_create(Badge<GraphicsManagement>);

    void disable_vga_emulation_access_permanently(Badge<GraphicsManagement>);
    void enable_vga_text_mode_console_cursor(Badge<GraphicsManagement>);
    void disable_vga_text_mode_console_cursor(Badge<GraphicsManagement>);
    void set_vga_text_mode_cursor(Badge<GraphicsManagement>, size_t console_width, size_t x, size_t y);

    void unblank_screen(Badge<GraphicsManagement>);

    ~VGAIOArbiter();

private:
    VGAIOArbiter();

    void disable_vga_text_mode_console_cursor();
    void enable_vga_text_mode_console_cursor();

    RecursiveSpinlock m_main_vga_lock { LockRank::None };
    bool m_vga_access_is_disabled { false };
};

}
