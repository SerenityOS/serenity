#include "i8253.h"
#include "i386.h"
#include "IO.h"
#include "PIC.h"
#include "Scheduler.h"

#define IRQ_TIMER                0

extern "C" void tick_ISR();
extern "C" void clock_handle(RegisterDump&);

asm(
    ".globl tick_ISR \n"
    "tick_ISR: \n"
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
    "    call clock_handle\n"
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

#define WRITE_word         0x30

/* Miscellaneous */
#define BASE_FREQUENCY     1193182

void clock_handle(RegisterDump& regs)
{
    IRQHandlerScope scope(IRQ_TIMER);
    Scheduler::timer_tick(regs);
}

namespace PIT {

void initialize()
{
    word timer_reload;

    IO::out8(PIT_CTL, TIMER0_SELECT | WRITE_word | MODE_SQUARE_WAVE);

    timer_reload = (BASE_FREQUENCY / TICKS_PER_SECOND);

    kprintf("PIT(i8253): %u Hz, square wave (%x)\n", TICKS_PER_SECOND, timer_reload);

    IO::out8(TIMER0_CTL, LSB(timer_reload));
    IO::out8(TIMER0_CTL, MSB(timer_reload));

    register_interrupt_handler(IRQ_VECTOR_BASE + IRQ_TIMER, tick_ISR);

    PIC::enable(IRQ_TIMER);
}

}
