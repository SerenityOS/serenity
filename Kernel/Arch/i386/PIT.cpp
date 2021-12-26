#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/PIC.h>
#include <Kernel/Arch/i386/PIT.h>
#include <Kernel/IO.h>
#include <Kernel/Scheduler.h>

#define IRQ_TIMER 0

extern "C" void timer_interrupt_entry();
extern "C" void timer_interrupt_handler(RegisterDump&);

asm(
    ".globl timer_interrupt_entry \n"
    "timer_interrupt_entry: \n"
    "    pushl $0x0\n"
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
    "    add $0x4, %esp\n"
    "    iret\n");

static u32 s_ticks_this_second;
static u32 s_seconds_since_boot;

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

u32 ticks_this_second()
{
    return s_ticks_this_second;
}

u32 seconds_since_boot()
{
    return s_seconds_since_boot;
}

void initialize()
{
    u16 timer_reload;

    IO::out8(PIT_CTL, TIMER0_SELECT | WRITE_WORD | MODE_SQUARE_WAVE);

    timer_reload = (BASE_FREQUENCY / TICKS_PER_SECOND);

    kprintf("PIT: %u Hz, square wave (%x)\n", TICKS_PER_SECOND, timer_reload);

    IO::out8(TIMER0_CTL, LSB(timer_reload));
    IO::out8(TIMER0_CTL, MSB(timer_reload));

    register_interrupt_handler(IRQ_VECTOR_BASE + IRQ_TIMER, timer_interrupt_entry);

    PIC::enable(IRQ_TIMER);
}

}
