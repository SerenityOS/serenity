/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/IO.h>
#include <Kernel/Arch/x86_64/PCSpeaker.h>

#define TIMER2_CTL 0x42
#define PIT_CTL 0x43

#define TIMER2_SELECT 0x80
#define WRITE_WORD 0x30
#define MODE_SQUARE_WAVE 0x06

#define BASE_FREQUENCY 1193182

void PCSpeaker::tone_on(int frequency)
{
    IO::out8(PIT_CTL, TIMER2_SELECT | WRITE_WORD | MODE_SQUARE_WAVE);
    u16 timer_reload = BASE_FREQUENCY / frequency;

    IO::out8(TIMER2_CTL, timer_reload & 0xFF);
    IO::out8(TIMER2_CTL, timer_reload >> 8);

    IO::out8(0x61, IO::in8(0x61) | 3);
}

void PCSpeaker::tone_off()
{
    IO::out8(0x61, IO::in8(0x61) & ~3);
}
