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

class GPUManagement;
class VGAIOArbiter {
public:
    static NonnullOwnPtr<VGAIOArbiter> must_create(Badge<GPUManagement>);

    void disable_vga_emulation_access_permanently(Badge<GPUManagement>);
    void enable_vga_text_mode_console_cursor(Badge<GPUManagement>);
    void disable_vga_text_mode_console_cursor(Badge<GPUManagement>);
    void set_vga_text_mode_cursor(Badge<GPUManagement>, size_t console_width, size_t x, size_t y);

    void unblank_screen(Badge<GPUManagement>);

    ~VGAIOArbiter();

private:
    VGAIOArbiter();

    void disable_vga_text_mode_console_cursor();
    void enable_vga_text_mode_console_cursor();

    RecursiveSpinlock<LockRank::None> m_main_vga_lock {};
    bool m_vga_access_is_disabled { false };
};

}
