/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/IO.h>
#include <Kernel/Arch/x86_64/PCSpeaker.h>
#include <Kernel/Arch/x86_64/Time/PIT.h>

void PCSpeaker::tone_on(int frequency)
{
    IO::out8(PIT_CTL, TIMER2_SELECT | WRITE_WORD | MODE_SQUARE_WAVE);
    u16 timer_reload = BASE_FREQUENCY / frequency;

    IO::out8(TIMER2_CTL, LSB(timer_reload));
    IO::out8(TIMER2_CTL, MSB(timer_reload));

    IO::out8(0x61, IO::in8(0x61) | 3);
}

void PCSpeaker::tone_off()
{
    IO::out8(0x61, IO::in8(0x61) & ~3);
}
