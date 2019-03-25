#include "i8253.h"
#include "i386.h"
#include "IO.h"
#include "PIC.h"
#include "Scheduler.h"

#define IRQ_TIMER 0

extern "C" void timer_interrupt_entry();
extern "C" void timer_interrupt_handler(RegisterDump&);

asm(
    ".globl timer_interrupt_entry \n"
    "timer_interrupt_entry: \n"
    "    pusha\n"
    "    pushw %ds\n"
    "    pushw %es\n"
    "    pushw %fs\n"
    "    pushw %gs\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    pushw %ss\n"
    "    popw %ds\n"
    "    popw %es\n"
    "    popw %fs\n"
    "    popw %gs\n"
    "    mov %esp, %eax\n"
    "    call timer_interrupt_handler\n"
    "    popw %gs\n"
    "    popw %gs\n"
    "    popw %fs\n"
    "    popw %es\n"
    "    popw %ds\n"
    "    popa\n"
    "    iret\n"
);

/* Timer related ports */
#define TIMER0_CTL         0x40
#define TIMER1_CTL         0x41
#define TIMER2_CTL         0x42
#define PIT_CTL            0x43

/* Building blocks for PIT_CTL */
#define TIMER0_SELECT      0x00
#define TIMER1_SELECT      0x40
#define TIMER2_SELECT      0x80

#define MODE_COUNTDOWN     0x00
#define MODE_ONESHOT       0x02
#define MODE_RATE          0x04
#define MODE_SQUARE_WAVE   0x06

#define WRITE_WORD         0x30

#define BASE_FREQUENCY     1193182

static dword s_ticks_this_second;
static dword s_seconds_since_boot;

void timer_interrupt_handler(RegisterDump& regs)
{
    IRQHandlerScope scope(IRQ_TIMER);
    if (++s_ticks_this_second >= TICKS_PER_SECOND) {
        // FIXME: Synchronize with the RTC somehow to prevent drifting apart.
        ++s_seconds_since_boot;
        s_ticks_this_second = 0;
    }
    Scheduler::timer_tick(regs);
}

namespace PIT {

dword ticks_this_second()
{
    return s_ticks_this_second;
}

dword seconds_since_boot()
{
    return s_seconds_since_boot;
}

void initialize()
{
    word timer_reload;

    IO::out8(PIT_CTL, TIMER0_SELECT | WRITE_WORD | MODE_SQUARE_WAVE);

    timer_reload = (BASE_FREQUENCY / TICKS_PER_SECOND);

    kprintf("PIT: %u Hz, square wave (%x)\n", TICKS_PER_SECOND, timer_reload);

    IO::out8(TIMER0_CTL, LSB(timer_reload));
    IO::out8(TIMER0_CTL, MSB(timer_reload));

    register_interrupt_handler(IRQ_VECTOR_BASE + IRQ_TIMER, timer_interrupt_entry);

    PIC::enable(IRQ_TIMER);
}

}
