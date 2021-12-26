#include <Kernel/Devices/PCSpeaker.h>
#include <Kernel/i8253.h>
#include <Kernel/IO.h>
#include <Kernel/i386.h>

void PCSpeaker::tone_on(int frequency)
{
    IO::out8(PIT_CTL, TIMER2_SELECT | WRITE_WORD | MODE_SQUARE_WAVE);
    word timer_reload = BASE_FREQUENCY / frequency;

    IO::out8(TIMER2_CTL, LSB(timer_reload));
    IO::out8(TIMER2_CTL, MSB(timer_reload));

    IO::out8(0x61, IO::in8(0x61) | 3);
}

void PCSpeaker::tone_off()
{
    IO::out8(0x61, IO::in8(0x61) & ~3);
}
