#include "i8253.h"
#include "i386.h"
#include "IO.h"
#include "VGA.h"
#include "Task.h"
#include "system.h"
#include "PIC.h"

#define IRQ_TIMER                0

extern "C" void tick_ISR();
extern "C" void clock_handle();

extern volatile DWORD state_dump;

asm(
    ".globl tick_ISR \n"
    ".globl state_dump \n"
    "state_dump: \n"
    ".long 0\n"
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
    "    popw %ds\n"
    "    popw %es\n"
    "    popw %fs\n"
    "    popw %gs\n"
    "    mov %esp, state_dump\n"
    "    call clock_handle\n"
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

/* Miscellaneous */
#define BASE_FREQUENCY     1193182

void clock_handle()
{
    IRQHandlerScope scope(IRQ_TIMER);

    if (!current)
        return;

    system.uptime++;

    if (current->tick())
        return;

    //return;

    auto& regs = *reinterpret_cast<RegisterDump*>(state_dump);
    current->tss().gs = regs.gs;
    current->tss().fs = regs.fs;
    current->tss().es = regs.es;
    current->tss().ds = regs.ds;
    current->tss().edi = regs.edi;
    current->tss().esi = regs.esi;
    current->tss().ebp = regs.ebp;
    current->tss().ebx = regs.ebx;
    current->tss().edx = regs.edx;
    current->tss().ecx = regs.ecx;
    current->tss().eax = regs.eax;
    current->tss().eip = regs.eip;
    current->tss().cs = regs.cs;
    current->tss().eflags = regs.eflags;

#if 0
    BYTE a = vga_get_attr();
    WORD foo = vga_get_cursor();

    vga_set_attr(0x50);
    vga_set_cursor(0);

    kprintf("\n\n");
    kprintf("Task %u interrupted at %x             \n", current->pid(), regs.eip );
    kprintf("EAX=%x EBX=%x ECX=%x EDX=%x           \n", regs.eax, regs.ebx, regs.ecx, regs.edx);
    kprintf("ESI=%x EDI=%x EBP=%x ESP=%x           \n", regs.esi, regs.edi, regs.ebp, regs.esp);
    kprintf("FLAGS=%x", regs.eflags);

    vga_set_cursor(foo);
    vga_set_attr(a);
#endif

    // Compute task ESP.
    // Add 12 for CS, EIP, EFLAGS (interrupt mechanic)

    // FIXME: Hmm. Should we add an extra 8 here for SS:ESP in some cases?
    //        If this IRQ occurred while in a user task, wouldn't that also push the stack ptr?
    current->tss().esp = regs.esp + 12;

    // Prepare a new task to run;
    if (!scheduleNewTask())
        return;
    Task::prepForIRETToNewTask();

    // Set the NT (nested task) flag.
    asm(
        "pushf\n"
        "orl $0x00004000, (%esp)\n"
        "popf\n"
    );
}

namespace PIT {

void initialize()
{
    WORD timer_reload;

    IO::out8(PIT_CTL, TIMER0_SELECT | WRITE_WORD | MODE_SQUARE_WAVE);

    timer_reload = (BASE_FREQUENCY / TICKS_PER_SECOND);

    /* Send LSB and MSB of timer reload value. */

    kprintf("PIT(i8253): %u Hz, square wave (%x)\n", TICKS_PER_SECOND, timer_reload);

    IO::out8(TIMER0_CTL, LSB(timer_reload));
    IO::out8(TIMER0_CTL, MSB(timer_reload));

    registerInterruptHandler(IRQ_VECTOR_BASE + IRQ_TIMER, tick_ISR);

    PIC::enable(IRQ_TIMER);
}

}
