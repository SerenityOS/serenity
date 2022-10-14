/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Try.h>
#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Arch/x86_64/IO.h>
#include <Kernel/Arch/x86_64/VGA/IOArbiter.h>

namespace Kernel {

NonnullOwnPtr<VGAIOArbiter> VGAIOArbiter::must_create(Badge<GraphicsManagement>)
{
    return MUST(adopt_nonnull_own_or_enomem(new (nothrow) VGAIOArbiter()));
}

VGAIOArbiter::~VGAIOArbiter() = default;
VGAIOArbiter::VGAIOArbiter() = default;

void VGAIOArbiter::disable_vga_emulation_access_permanently(Badge<GraphicsManagement>)
{
    SpinlockLocker locker(m_main_vga_lock);
    disable_vga_text_mode_console_cursor();
    IO::out8(0x3c4, 1);
    u8 sr1 = IO::in8(0x3c5);
    IO::out8(0x3c5, sr1 | 1 << 5);
    microseconds_delay(1000);
    m_vga_access_is_disabled = true;
}

void VGAIOArbiter::enable_vga_text_mode_console_cursor(Badge<GraphicsManagement>)
{
    enable_vga_text_mode_console_cursor();
}

void VGAIOArbiter::enable_vga_text_mode_console_cursor()
{
    SpinlockLocker locker(m_main_vga_lock);
    if (m_vga_access_is_disabled)
        return;
    IO::out8(0x3D4, 0xA);
    IO::out8(0x3D5, 0);
}

void VGAIOArbiter::disable_vga_text_mode_console_cursor(Badge<GraphicsManagement>)
{
    disable_vga_text_mode_console_cursor();
}

void VGAIOArbiter::disable_vga_text_mode_console_cursor()
{
    SpinlockLocker locker(m_main_vga_lock);
    if (m_vga_access_is_disabled)
        return;
    IO::out8(0x3D4, 0xA);
    IO::out8(0x3D5, 0x20);
}

void VGAIOArbiter::unblank_screen(Badge<GraphicsManagement>)
{
    SpinlockLocker locker(m_main_vga_lock);
    if (m_vga_access_is_disabled)
        return;
    IO::out8(0x3c0, 0x20);
}

void VGAIOArbiter::set_vga_text_mode_cursor(Badge<GraphicsManagement>, size_t console_width, size_t x, size_t y)
{
    SpinlockLocker locker(m_main_vga_lock);
    if (m_vga_access_is_disabled)
        return;
    enable_vga_text_mode_console_cursor();
    u16 value = y * console_width + x;
    IO::out8(0x3d4, 0x0e);
    IO::out8(0x3d5, MSB(value));
    IO::out8(0x3d4, 0x0f);
    IO::out8(0x3d5, LSB(value));
}

}
