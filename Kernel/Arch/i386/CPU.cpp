/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/ISRStubs.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>
#include <Kernel/Interrupts/SpuriousInterruptHandler.h>
#include <Kernel/Interrupts/UnhandledInterruptHandler.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/mallocdefs.h>

//#define PAGE_FAULT_DEBUG

namespace Kernel {

struct [[gnu::packed]] DescriptorTablePointer
{
    u16 limit;
    void* address;
};

static DescriptorTablePointer s_idtr;
static DescriptorTablePointer s_gdtr;
static Descriptor s_idt[256];
static Descriptor s_gdt[256];

static GenericInterruptHandler* s_interrupt_handler[GENERIC_INTERRUPT_HANDLERS_COUNT];

static Vector<u16>* s_gdt_freelist;

static u16 s_gdt_length;

u16 gdt_alloc_entry()
{
    ASSERT(s_gdt_freelist);
    ASSERT(!s_gdt_freelist->is_empty());
    return s_gdt_freelist->take_last();
}

void gdt_free_entry(u16 entry)
{
    s_gdt_freelist->append(entry);
}

extern "C" void handle_interrupt(RegisterState);

#define EH_ENTRY(ec, title)                         \
    extern "C" void title##_asm_entry();            \
    extern "C" void title##_handler(RegisterState); \
    asm(                                            \
        ".globl " #title "_asm_entry\n"             \
        "" #title "_asm_entry: \n"                  \
        "    pusha\n"                               \
        "    pushl %ds\n"                           \
        "    pushl %es\n"                           \
        "    pushl %fs\n"                           \
        "    pushl %gs\n"                           \
        "    pushl %ss\n"                           \
        "    mov $0x10, %ax\n"                      \
        "    mov %ax, %ds\n"                        \
        "    mov %ax, %es\n"                        \
        "    cld\n"                                 \
        "    call " #title "_handler\n"             \
        "    add $0x4, %esp \n"                     \
        "    popl %gs\n"                            \
        "    popl %fs\n"                            \
        "    popl %es\n"                            \
        "    popl %ds\n"                            \
        "    popa\n"                                \
        "    add $0x4, %esp\n"                      \
        "    iret\n");

#define EH_ENTRY_NO_CODE(ec, title)                 \
    extern "C" void title##_handler(RegisterState); \
    extern "C" void title##_asm_entry();            \
    asm(                                            \
        ".globl " #title "_asm_entry\n"             \
        "" #title "_asm_entry: \n"                  \
        "    pushl $0x0\n"                          \
        "    pusha\n"                               \
        "    pushl %ds\n"                           \
        "    pushl %es\n"                           \
        "    pushl %fs\n"                           \
        "    pushl %gs\n"                           \
        "    pushl %ss\n"                           \
        "    mov $0x10, %ax\n"                      \
        "    mov %ax, %ds\n"                        \
        "    mov %ax, %es\n"                        \
        "    cld\n"                                 \
        "    call " #title "_handler\n"             \
        "    add $0x4, %esp\n"                      \
        "    popl %gs\n"                            \
        "    popl %fs\n"                            \
        "    popl %es\n"                            \
        "    popl %ds\n"                            \
        "    popa\n"                                \
        "    add $0x4, %esp\n"                      \
        "    iret\n");

static void dump(const RegisterState& regs)
{
    u16 ss;
    u32 esp;
    if (!Process::current || Process::current->is_ring0()) {
        ss = regs.ss;
        esp = regs.esp;
    } else {
        ss = regs.userspace_ss;
        esp = regs.userspace_esp;
    }

    klog() << "exception code: " << String::format("%04x", regs.exception_code) << " (isr: " << String::format("%04x", regs.isr_number);
    klog() << "  pc=" << String::format("%04x", (u16)regs.cs) << ":" << String::format("%08x", regs.eip) << " flags=" << String::format("%04x", (u16)regs.eflags);
    klog() << " stk=" << String::format("%04x", ss) << ":" << String::format("%08x", esp);
    klog() << "  ds=" << String::format("%04x", (u16)regs.ds) << " es=" << String::format("%04x", (u16)regs.es) << " fs=" << String::format("%04x", (u16)regs.fs) << " gs=" << String::format("%04x", (u16)regs.gs);
    klog() << "eax=" << String::format("%08x", regs.eax) << " ebx=" << String::format("%08x", regs.ebx) << " ecx=" << String::format("%08x", regs.ecx) << " edx=" << String::format("%08x", regs.edx);
    klog() << "ebp=" << String::format("%08x", regs.ebp) << " esp=" << String::format("%08x", regs.esp) << " esi=" << String::format("%08x", regs.esi) << " edi=" << String::format("%08x", regs.edi);
    u32 cr0;
    asm("movl %%cr0, %%eax"
        : "=a"(cr0));
    u32 cr2;
    asm("movl %%cr2, %%eax"
        : "=a"(cr2));
    u32 cr3 = read_cr3();
    u32 cr4;
    asm("movl %%cr4, %%eax"
        : "=a"(cr4));
    klog() << "cr0=" << String::format("%08x", cr0) << " cr2=" << String::format("%08x", cr2) << " cr3=" << String::format("%08x", cr3) << " cr4=" << String::format("%08x", cr4);

    if (Process::current && Process::current->validate_read((void*)regs.eip, 8)) {
        SmapDisabler disabler;
        u8* codeptr = (u8*)regs.eip;
        klog() << "code: " << String::format("%02x", codeptr[0]) << " " << String::format("%02x", codeptr[1]) << " " << String::format("%02x", codeptr[2]) << " " << String::format("%02x", codeptr[3]) << " " << String::format("%02x", codeptr[4]) << " " << String::format("%02x", codeptr[5]) << " " << String::format("%02x", codeptr[6]) << " " << String::format("%02x", codeptr[7]);
    }
}

void handle_crash(RegisterState& regs, const char* description, int signal)
{
    if (!Process::current) {
        klog() << description << " with !current";
        hang();
    }

    // If a process crashed while inspecting another process,
    // make sure we switch back to the right page tables.
    MM.enter_process_paging_scope(*Process::current);

    klog() << "CRASH: " << description << ". " << (Process::current->is_ring0() ? "Kernel" : "Process") << ": " << Process::current->name().characters() << "(" << Process::current->pid() << ")";
    dump(regs);

    if (Process::current->is_ring0()) {
        klog() << "Oh shit, we've crashed in ring 0 :(";
        dump_backtrace();
        hang();
    }

    cli();
    Process::current->crash(signal, regs.eip);
}

EH_ENTRY_NO_CODE(6, illegal_instruction);
void illegal_instruction_handler(RegisterState regs)
{
    clac();
    handle_crash(regs, "Illegal instruction", SIGILL);
}

EH_ENTRY_NO_CODE(0, divide_error);
void divide_error_handler(RegisterState regs)
{
    clac();
    handle_crash(regs, "Divide error", SIGFPE);
}

EH_ENTRY(13, general_protection_fault);
void general_protection_fault_handler(RegisterState regs)
{
    clac();
    handle_crash(regs, "General protection fault", SIGSEGV);
}

// 7: FPU not available exception
EH_ENTRY_NO_CODE(7, fpu_exception);
void fpu_exception_handler(RegisterState)
{
    // Just clear the TS flag. We've already restored the FPU state eagerly.
    // FIXME: It would be nice if we didn't have to do this at all.
    asm volatile("clts");
}

// 14: Page Fault
EH_ENTRY(14, page_fault);
void page_fault_handler(RegisterState regs)
{
    clac();

    u32 fault_address;
    asm("movl %%cr2, %%eax"
        : "=a"(fault_address));

#ifdef PAGE_FAULT_DEBUG
    u32 fault_page_directory = read_cr3();
    dbg() << "Ring " << (regs.cs & 3)
          << " " << (regs.exception_code & 1 ? "PV" : "NP")
          << " page fault in PD=" << String::format("%x", fault_page_directory) << ", "
          << (regs.exception_code & 8 ? "reserved-bit " : "")
          << (regs.exception_code & 2 ? "write" : "read")
          << " " << VirtualAddress(fault_address);
#endif

#ifdef PAGE_FAULT_DEBUG
    dump(regs);
#endif

    bool faulted_in_userspace = (regs.cs & 3) == 3;
    if (faulted_in_userspace && !MM.validate_user_stack(*Process::current, VirtualAddress(regs.userspace_esp))) {
        dbg() << "Invalid stack pointer: " << String::format("%p", regs.userspace_esp);
        handle_crash(regs, "Bad stack on page fault", SIGSTKFLT);
        ASSERT_NOT_REACHED();
    }

    auto response = MM.handle_page_fault(PageFault(regs.exception_code, VirtualAddress(fault_address)));

    if (response == PageFaultResponse::ShouldCrash) {
        if (Thread::current->has_signal_handler(SIGSEGV)) {
            Thread::current->send_urgent_signal_to_self(SIGSEGV);
            return;
        }

        klog() << "tid - (" << Thread::current->tid() << ") Unrecoverable page fault, " << (regs.exception_code & PageFaultFlags::ReservedBitViolation ? "reserved bit violation / " : "") << ":" << (regs.exception_code & PageFaultFlags::InstructionFetch ? "instruction fetch / " : "") << ":" << (regs.exception_code & PageFaultFlags::Write ? "write to" : "read from") << " address " << String::format("%p", fault_address);
        u32 malloc_scrub_pattern = explode_byte(MALLOC_SCRUB_BYTE);
        u32 free_scrub_pattern = explode_byte(FREE_SCRUB_BYTE);
        u32 kmalloc_scrub_pattern = explode_byte(KMALLOC_SCRUB_BYTE);
        u32 kfree_scrub_pattern = explode_byte(KFREE_SCRUB_BYTE);
        u32 slab_alloc_scrub_pattern = explode_byte(SLAB_ALLOC_SCRUB_BYTE);
        u32 slab_dealloc_scrub_pattern = explode_byte(SLAB_DEALLOC_SCRUB_BYTE);
        if ((fault_address & 0xffff0000) == (malloc_scrub_pattern & 0xffff0000)) {
            klog() << "Note: Address " << String::format("%p", fault_address) << " looks like it may be uninitialized malloc() memory";
        } else if ((fault_address & 0xffff0000) == (free_scrub_pattern & 0xffff0000)) {
            klog() << "Note: Address " << String::format("%p", fault_address) << " looks like it may be recently free()'d memory";
        } else if ((fault_address & 0xffff0000) == (kmalloc_scrub_pattern & 0xffff0000)) {
            klog() << "Note: Address " << String::format("%p", fault_address) << " looks like it may be uninitialized kmalloc() memory";
        } else if ((fault_address & 0xffff0000) == (kfree_scrub_pattern & 0xffff0000)) {
            klog() << "Note: Address " << String::format("%p", fault_address) << " looks like it may be recently kfree()'d memory";
        } else if ((fault_address & 0xffff0000) == (slab_alloc_scrub_pattern & 0xffff0000)) {
            klog() << "Note: Address " << String::format("%p", fault_address) << " looks like it may be uninitialized slab_alloc() memory";
        } else if ((fault_address & 0xffff0000) == (slab_dealloc_scrub_pattern & 0xffff0000)) {
            klog() << "Note: Address " << String::format("%p", fault_address) << " looks like it may be recently slab_dealloc()'d memory";
        } else if (fault_address < 4096) {
            klog() << "Note: Address " << String::format("%p", fault_address) << " looks like a possible nullptr dereference";
        }

        handle_crash(regs, "Page Fault", SIGSEGV);
    } else if (response == PageFaultResponse::Continue) {
#ifdef PAGE_FAULT_DEBUG
        dbg() << "Continuing after resolved page fault";
#endif
    } else {
        ASSERT_NOT_REACHED();
    }
}

#define EH(i, msg)                                                                                                                                                             \
    static void _exception##i()                                                                                                                                                \
    {                                                                                                                                                                          \
        klog() << msg;                                                                                                                                                         \
        u32 cr0, cr2, cr3, cr4;                                                                                                                                                \
        asm("movl %%cr0, %%eax"                                                                                                                                                \
            : "=a"(cr0));                                                                                                                                                      \
        asm("movl %%cr2, %%eax"                                                                                                                                                \
            : "=a"(cr2));                                                                                                                                                      \
        asm("movl %%cr3, %%eax"                                                                                                                                                \
            : "=a"(cr3));                                                                                                                                                      \
        asm("movl %%cr4, %%eax"                                                                                                                                                \
            : "=a"(cr4));                                                                                                                                                      \
        klog() << "CR0=" << String::format("%x", cr0) << " CR2=" << String::format("%x", cr2) << " CR3=" << String::format("%x", cr3) << " CR4=" << String::format("%x", cr4); \
        hang();                                                                                                                                                                \
    }

EH(1, "Debug exception")
EH(2, "Unknown error")
EH(3, "Breakpoint")
EH(4, "Overflow")
EH(5, "Bounds check")
EH(8, "Double fault")
EH(9, "Coprocessor segment overrun")
EH(10, "Invalid TSS")
EH(11, "Segment not present")
EH(12, "Stack exception")
EH(15, "Unknown error")
EH(16, "Coprocessor error")

static void write_raw_gdt_entry(u16 selector, u32 low, u32 high)
{
    u16 i = (selector & 0xfffc) >> 3;
    s_gdt[i].low = low;
    s_gdt[i].high = high;

    if (i > s_gdt_length)
        s_gdtr.limit = (s_gdt_length + 1) * 8 - 1;
}

void write_gdt_entry(u16 selector, Descriptor& descriptor)
{
    write_raw_gdt_entry(selector, descriptor.low, descriptor.high);
}

Descriptor& get_gdt_entry(u16 selector)
{
    u16 i = (selector & 0xfffc) >> 3;
    return *(Descriptor*)(&s_gdt[i]);
}

void flush_gdt()
{
    s_gdtr.address = s_gdt;
    s_gdtr.limit = (s_gdt_length * 8) - 1;
    asm("lgdt %0" ::"m"(s_gdtr)
        : "memory");
}

void gdt_init()
{
    s_gdt_length = 5;

    s_gdt_freelist = new Vector<u16>();
    s_gdt_freelist->ensure_capacity(256);
    for (size_t i = s_gdt_length; i < 256; ++i)
        s_gdt_freelist->append(i * 8);

    s_gdt_length = 256;
    s_gdtr.address = s_gdt;
    s_gdtr.limit = (s_gdt_length * 8) - 1;

    write_raw_gdt_entry(0x0000, 0x00000000, 0x00000000);
    write_raw_gdt_entry(0x0008, 0x0000ffff, 0x00cf9a00);
    write_raw_gdt_entry(0x0010, 0x0000ffff, 0x00cf9200);
    write_raw_gdt_entry(0x0018, 0x0000ffff, 0x00cffa00);
    write_raw_gdt_entry(0x0020, 0x0000ffff, 0x00cff200);

    flush_gdt();

    asm volatile(
        "mov %%ax, %%ds\n"
        "mov %%ax, %%es\n"
        "mov %%ax, %%fs\n"
        "mov %%ax, %%gs\n"
        "mov %%ax, %%ss\n" ::"a"(0x10)
        : "memory");

    // Make sure CS points to the kernel code descriptor.
    asm volatile(
        "ljmpl $0x8, $sanity\n"
        "sanity:\n");
}

static void unimp_trap()
{
    klog() << "Unhandled IRQ.";
    hang();
}

GenericInterruptHandler& get_interrupt_handler(u8 interrupt_number)
{
    ASSERT(s_interrupt_handler[interrupt_number] != nullptr);
    return *s_interrupt_handler[interrupt_number];
}

static void revert_to_unused_handler(u8 interrupt_number)
{
    new UnhandledInterruptHandler(interrupt_number);
}

void register_generic_interrupt_handler(u8 interrupt_number, GenericInterruptHandler& handler)
{
    if (s_interrupt_handler[interrupt_number] != nullptr) {
        if (s_interrupt_handler[interrupt_number]->purpose() == HandlerPurpose::UnhandledInterruptHandler) {
            s_interrupt_handler[interrupt_number] = &handler;
            return;
        }
        if (s_interrupt_handler[interrupt_number]->is_shared_handler() && !s_interrupt_handler[interrupt_number]->is_sharing_with_others()) {
            ASSERT(s_interrupt_handler[interrupt_number]->purpose() == HandlerPurpose::SharedIRQHandler);
            static_cast<SharedIRQHandler*>(s_interrupt_handler[interrupt_number])->register_handler(handler);
            return;
        }
        if (!s_interrupt_handler[interrupt_number]->is_shared_handler()) {
            ASSERT(s_interrupt_handler[interrupt_number]->purpose() == HandlerPurpose::IRQHandler);
            auto& previous_handler = *s_interrupt_handler[interrupt_number];
            s_interrupt_handler[interrupt_number] = nullptr;
            SharedIRQHandler::initialize(interrupt_number);
            static_cast<SharedIRQHandler*>(s_interrupt_handler[interrupt_number])->register_handler(previous_handler);
            static_cast<SharedIRQHandler*>(s_interrupt_handler[interrupt_number])->register_handler(handler);
            return;
        }
        ASSERT_NOT_REACHED();
    } else {
        s_interrupt_handler[interrupt_number] = &handler;
    }
}

void unregister_generic_interrupt_handler(u8 interrupt_number, GenericInterruptHandler& handler)
{
    ASSERT(s_interrupt_handler[interrupt_number] != nullptr);
    if (s_interrupt_handler[interrupt_number]->purpose() == HandlerPurpose::UnhandledInterruptHandler) {
        dbg() << "Trying to unregister unused handler (?)";
        return;
    }
    if (s_interrupt_handler[interrupt_number]->is_shared_handler() && !s_interrupt_handler[interrupt_number]->is_sharing_with_others()) {
        ASSERT(s_interrupt_handler[interrupt_number]->purpose() == HandlerPurpose::SharedIRQHandler);
        static_cast<SharedIRQHandler*>(s_interrupt_handler[interrupt_number])->unregister_handler(handler);
        if (!static_cast<SharedIRQHandler*>(s_interrupt_handler[interrupt_number])->sharing_devices_count()) {
            revert_to_unused_handler(interrupt_number);
        }
        return;
    }
    if (!s_interrupt_handler[interrupt_number]->is_shared_handler()) {
        ASSERT(s_interrupt_handler[interrupt_number]->purpose() == HandlerPurpose::IRQHandler);
        revert_to_unused_handler(interrupt_number);
        return;
    }
    ASSERT_NOT_REACHED();
}

void register_interrupt_handler(u8 index, void (*f)())
{
    s_idt[index].low = 0x00080000 | LSW((f));
    s_idt[index].high = ((u32)(f)&0xffff0000) | 0x8e00;
}

void register_user_callable_interrupt_handler(u8 index, void (*f)())
{
    s_idt[index].low = 0x00080000 | LSW((f));
    s_idt[index].high = ((u32)(f)&0xffff0000) | 0xef00;
}

void flush_idt()
{
    asm("lidt %0" ::"m"(s_idtr));
}

void idt_init()
{
    s_idtr.address = s_idt;
    s_idtr.limit = 0x100 * 8 - 1;

    for (u8 i = 0xff; i > 0x10; --i)
        register_interrupt_handler(i, unimp_trap);

    register_interrupt_handler(0x00, divide_error_asm_entry);
    register_interrupt_handler(0x01, _exception1);
    register_interrupt_handler(0x02, _exception2);
    register_interrupt_handler(0x03, _exception3);
    register_interrupt_handler(0x04, _exception4);
    register_interrupt_handler(0x05, _exception5);
    register_interrupt_handler(0x06, illegal_instruction_asm_entry);
    register_interrupt_handler(0x07, fpu_exception_asm_entry);
    register_interrupt_handler(0x08, _exception8);
    register_interrupt_handler(0x09, _exception9);
    register_interrupt_handler(0x0a, _exception10);
    register_interrupt_handler(0x0b, _exception11);
    register_interrupt_handler(0x0c, _exception12);
    register_interrupt_handler(0x0d, general_protection_fault_asm_entry);
    register_interrupt_handler(0x0e, page_fault_asm_entry);
    register_interrupt_handler(0x0f, _exception15);
    register_interrupt_handler(0x10, _exception16);

    register_interrupt_handler(0x50, interrupt_0_asm_entry);
    register_interrupt_handler(0x51, interrupt_1_asm_entry);
    register_interrupt_handler(0x52, interrupt_2_asm_entry);
    register_interrupt_handler(0x53, interrupt_3_asm_entry);
    register_interrupt_handler(0x54, interrupt_4_asm_entry);
    register_interrupt_handler(0x55, interrupt_5_asm_entry);
    register_interrupt_handler(0x56, interrupt_6_asm_entry);
    register_interrupt_handler(0x57, interrupt_7_asm_entry);
    register_interrupt_handler(0x58, interrupt_8_asm_entry);
    register_interrupt_handler(0x59, interrupt_9_asm_entry);
    register_interrupt_handler(0x5a, interrupt_10_asm_entry);
    register_interrupt_handler(0x5b, interrupt_11_asm_entry);
    register_interrupt_handler(0x5c, interrupt_12_asm_entry);
    register_interrupt_handler(0x5d, interrupt_13_asm_entry);
    register_interrupt_handler(0x5e, interrupt_14_asm_entry);
    register_interrupt_handler(0x5f, interrupt_15_asm_entry);
    register_interrupt_handler(0x60, interrupt_16_asm_entry);
    register_interrupt_handler(0x61, interrupt_17_asm_entry);
    register_interrupt_handler(0x62, interrupt_18_asm_entry);
    register_interrupt_handler(0x63, interrupt_19_asm_entry);
    register_interrupt_handler(0x64, interrupt_20_asm_entry);
    register_interrupt_handler(0x65, interrupt_21_asm_entry);
    register_interrupt_handler(0x66, interrupt_22_asm_entry);
    register_interrupt_handler(0x67, interrupt_23_asm_entry);
    register_interrupt_handler(0x68, interrupt_24_asm_entry);
    register_interrupt_handler(0x69, interrupt_25_asm_entry);
    register_interrupt_handler(0x6a, interrupt_26_asm_entry);
    register_interrupt_handler(0x6b, interrupt_27_asm_entry);
    register_interrupt_handler(0x6c, interrupt_28_asm_entry);
    register_interrupt_handler(0x6d, interrupt_29_asm_entry);
    register_interrupt_handler(0x6e, interrupt_30_asm_entry);
    register_interrupt_handler(0x6f, interrupt_31_asm_entry);
    register_interrupt_handler(0x70, interrupt_32_asm_entry);
    register_interrupt_handler(0x71, interrupt_33_asm_entry);
    register_interrupt_handler(0x72, interrupt_34_asm_entry);
    register_interrupt_handler(0x73, interrupt_35_asm_entry);
    register_interrupt_handler(0x74, interrupt_36_asm_entry);
    register_interrupt_handler(0x75, interrupt_37_asm_entry);
    register_interrupt_handler(0x76, interrupt_38_asm_entry);
    register_interrupt_handler(0x77, interrupt_39_asm_entry);
    register_interrupt_handler(0x78, interrupt_40_asm_entry);
    register_interrupt_handler(0x79, interrupt_41_asm_entry);
    register_interrupt_handler(0x7a, interrupt_42_asm_entry);
    register_interrupt_handler(0x7b, interrupt_43_asm_entry);
    register_interrupt_handler(0x7c, interrupt_44_asm_entry);
    register_interrupt_handler(0x7d, interrupt_45_asm_entry);
    register_interrupt_handler(0x7e, interrupt_46_asm_entry);
    register_interrupt_handler(0x7f, interrupt_47_asm_entry);
    register_interrupt_handler(0x80, interrupt_48_asm_entry);
    register_interrupt_handler(0x81, interrupt_49_asm_entry);
    register_interrupt_handler(0x82, interrupt_50_asm_entry);
    register_interrupt_handler(0x83, interrupt_51_asm_entry);
    register_interrupt_handler(0x84, interrupt_52_asm_entry);
    register_interrupt_handler(0x85, interrupt_53_asm_entry);
    register_interrupt_handler(0x86, interrupt_54_asm_entry);
    register_interrupt_handler(0x87, interrupt_55_asm_entry);
    register_interrupt_handler(0x88, interrupt_56_asm_entry);
    register_interrupt_handler(0x89, interrupt_57_asm_entry);
    register_interrupt_handler(0x8a, interrupt_58_asm_entry);
    register_interrupt_handler(0x8b, interrupt_59_asm_entry);
    register_interrupt_handler(0x8c, interrupt_60_asm_entry);
    register_interrupt_handler(0x8d, interrupt_61_asm_entry);
    register_interrupt_handler(0x8e, interrupt_62_asm_entry);
    register_interrupt_handler(0x8f, interrupt_63_asm_entry);
    register_interrupt_handler(0x90, interrupt_64_asm_entry);
    register_interrupt_handler(0x91, interrupt_65_asm_entry);
    register_interrupt_handler(0x92, interrupt_66_asm_entry);
    register_interrupt_handler(0x93, interrupt_67_asm_entry);
    register_interrupt_handler(0x94, interrupt_68_asm_entry);
    register_interrupt_handler(0x95, interrupt_69_asm_entry);
    register_interrupt_handler(0x96, interrupt_70_asm_entry);
    register_interrupt_handler(0x97, interrupt_71_asm_entry);
    register_interrupt_handler(0x98, interrupt_72_asm_entry);
    register_interrupt_handler(0x99, interrupt_73_asm_entry);
    register_interrupt_handler(0x9a, interrupt_74_asm_entry);
    register_interrupt_handler(0x9b, interrupt_75_asm_entry);
    register_interrupt_handler(0x9c, interrupt_76_asm_entry);
    register_interrupt_handler(0x9d, interrupt_77_asm_entry);
    register_interrupt_handler(0x9e, interrupt_78_asm_entry);
    register_interrupt_handler(0x9f, interrupt_79_asm_entry);
    register_interrupt_handler(0xa0, interrupt_80_asm_entry);
    register_interrupt_handler(0xa1, interrupt_81_asm_entry);
    register_interrupt_handler(0xa2, interrupt_82_asm_entry);
    register_interrupt_handler(0xa3, interrupt_83_asm_entry);
    register_interrupt_handler(0xa4, interrupt_84_asm_entry);
    register_interrupt_handler(0xa5, interrupt_85_asm_entry);
    register_interrupt_handler(0xa6, interrupt_86_asm_entry);
    register_interrupt_handler(0xa7, interrupt_87_asm_entry);
    register_interrupt_handler(0xa8, interrupt_88_asm_entry);
    register_interrupt_handler(0xa9, interrupt_89_asm_entry);
    register_interrupt_handler(0xaa, interrupt_90_asm_entry);
    register_interrupt_handler(0xab, interrupt_91_asm_entry);
    register_interrupt_handler(0xac, interrupt_92_asm_entry);
    register_interrupt_handler(0xad, interrupt_93_asm_entry);
    register_interrupt_handler(0xae, interrupt_94_asm_entry);
    register_interrupt_handler(0xaf, interrupt_95_asm_entry);
    register_interrupt_handler(0xb0, interrupt_96_asm_entry);
    register_interrupt_handler(0xb1, interrupt_97_asm_entry);
    register_interrupt_handler(0xb2, interrupt_98_asm_entry);
    register_interrupt_handler(0xb3, interrupt_99_asm_entry);
    register_interrupt_handler(0xb4, interrupt_100_asm_entry);
    register_interrupt_handler(0xb5, interrupt_101_asm_entry);
    register_interrupt_handler(0xb6, interrupt_102_asm_entry);
    register_interrupt_handler(0xb7, interrupt_103_asm_entry);
    register_interrupt_handler(0xb8, interrupt_104_asm_entry);
    register_interrupt_handler(0xb9, interrupt_105_asm_entry);
    register_interrupt_handler(0xba, interrupt_106_asm_entry);
    register_interrupt_handler(0xbb, interrupt_107_asm_entry);
    register_interrupt_handler(0xbc, interrupt_108_asm_entry);
    register_interrupt_handler(0xbd, interrupt_109_asm_entry);
    register_interrupt_handler(0xbe, interrupt_110_asm_entry);
    register_interrupt_handler(0xbf, interrupt_111_asm_entry);
    register_interrupt_handler(0xc0, interrupt_112_asm_entry);
    register_interrupt_handler(0xc1, interrupt_113_asm_entry);
    register_interrupt_handler(0xc2, interrupt_114_asm_entry);
    register_interrupt_handler(0xc3, interrupt_115_asm_entry);
    register_interrupt_handler(0xc4, interrupt_116_asm_entry);
    register_interrupt_handler(0xc5, interrupt_117_asm_entry);
    register_interrupt_handler(0xc6, interrupt_118_asm_entry);
    register_interrupt_handler(0xc7, interrupt_119_asm_entry);
    register_interrupt_handler(0xc8, interrupt_120_asm_entry);
    register_interrupt_handler(0xc9, interrupt_121_asm_entry);
    register_interrupt_handler(0xca, interrupt_122_asm_entry);
    register_interrupt_handler(0xcb, interrupt_123_asm_entry);
    register_interrupt_handler(0xcc, interrupt_124_asm_entry);
    register_interrupt_handler(0xcd, interrupt_125_asm_entry);
    register_interrupt_handler(0xce, interrupt_126_asm_entry);
    register_interrupt_handler(0xcf, interrupt_127_asm_entry);

    for (u8 i = 0; i < GENERIC_INTERRUPT_HANDLERS_COUNT; ++i) {
        new UnhandledInterruptHandler(i);
    }

    flush_idt();
}

void load_task_register(u16 selector)
{
    asm("ltr %0" ::"r"(selector));
}

u32 g_in_irq;

void handle_interrupt(RegisterState regs)
{
    clac();
    ++g_in_irq;
    ASSERT(regs.isr_number >= 0x50 && regs.isr_number <= 0x5f);
    u8 irq = (u8)(regs.isr_number - 0x50);
    ASSERT(s_interrupt_handler[irq]);
    s_interrupt_handler[irq]->handle_interrupt(regs);
    s_interrupt_handler[irq]->increment_invoking_counter();
    s_interrupt_handler[irq]->eoi();
    --g_in_irq;
}

void sse_init()
{
    asm volatile(
        "mov %cr0, %eax\n"
        "andl $0xfffffffb, %eax\n"
        "orl $0x2, %eax\n"
        "mov %eax, %cr0\n"
        "mov %cr4, %eax\n"
        "orl $0x600, %eax\n"
        "mov %eax, %cr4\n");
}

bool g_cpu_supports_nx;
bool g_cpu_supports_pae;
bool g_cpu_supports_pge;
bool g_cpu_supports_rdrand;
bool g_cpu_supports_smap;
bool g_cpu_supports_smep;
bool g_cpu_supports_sse;
bool g_cpu_supports_tsc;
bool g_cpu_supports_umip;

void cpu_detect()
{
    CPUID processor_info(0x1);
    g_cpu_supports_pae = (processor_info.edx() & (1 << 6));
    g_cpu_supports_pge = (processor_info.edx() & (1 << 13));
    g_cpu_supports_sse = (processor_info.edx() & (1 << 25));
    g_cpu_supports_tsc = (processor_info.edx() & (1 << 4));
    g_cpu_supports_rdrand = (processor_info.ecx() & (1 << 30));

    CPUID extended_processor_info(0x80000001);
    g_cpu_supports_nx = (extended_processor_info.edx() & (1 << 20));

    CPUID extended_features(0x7);
    g_cpu_supports_smap = (extended_features.ebx() & (1 << 20));
    g_cpu_supports_smep = (extended_features.ebx() & (1 << 7));
    g_cpu_supports_umip = (extended_features.ecx() & (1 << 2));
}

void stac()
{
    if (!g_cpu_supports_smap)
        return;
    asm volatile("stac" ::
                     : "cc");
}

void clac()
{
    if (!g_cpu_supports_smap)
        return;
    asm volatile("clac" ::
                     : "cc");
}

void cpu_setup()
{
    cpu_detect();

    if (g_cpu_supports_sse) {
        sse_init();
        klog() << "x86: SSE support enabled";
    }

    asm volatile(
        "movl %%cr0, %%eax\n"
        "orl $0x00010000, %%eax\n"
        "movl %%eax, %%cr0\n" ::
            : "%eax", "memory");
    klog() << "x86: WP support enabled";

    if (g_cpu_supports_pge) {
        // Turn on CR4.PGE so the CPU will respect the G bit in page tables.
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x80, %eax\n"
            "mov %eax, %cr4\n");
        klog() << "x86: PGE support enabled";
    } else {
        klog() << "x86: PGE support not detected";
    }

    if (g_cpu_supports_nx) {
        // Turn on IA32_EFER.NXE
        asm volatile(
            "movl $0xc0000080, %ecx\n"
            "rdmsr\n"
            "orl $0x800, %eax\n"
            "wrmsr\n");
        klog() << "x86: NX support enabled";
    } else {
        klog() << "x86: NX support not detected";
    }

    if (g_cpu_supports_smep) {
        // Turn on CR4.SMEP
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x100000, %eax\n"
            "mov %eax, %cr4\n");
        klog() << "x86: SMEP support enabled";
    } else {
        klog() << "x86: SMEP support not detected";
    }

    if (g_cpu_supports_smap) {
        // Turn on CR4.SMAP
        klog() << "x86: Enabling SMAP";
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x200000, %eax\n"
            "mov %eax, %cr4\n");
        klog() << "x86: SMAP support enabled";
    } else {
        klog() << "x86: SMAP support not detected";
    }

    if (g_cpu_supports_umip) {
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x800, %eax\n"
            "mov %eax, %cr4\n");
        klog() << "x86: UMIP support enabled";
    }

    if (g_cpu_supports_tsc) {
        asm volatile(
            "mov %cr4, %eax\n"
            "orl $0x4, %eax\n"
            "mov %eax, %cr4\n");
        klog() << "x86: RDTSC support restricted";
    }

    if (g_cpu_supports_rdrand) {
        klog() << "x86: Using RDRAND for good randomness";
    } else {
        klog() << "x86: No RDRAND support detected. Randomness will be shitty";
    }
}

u32 read_cr3()
{
    u32 cr3;
    asm("movl %%cr3, %%eax"
        : "=a"(cr3));
    return cr3;
}

void write_cr3(u32 cr3)
{
    asm volatile("movl %%eax, %%cr3" ::"a"(cr3)
                 : "memory");
}

}

#ifdef DEBUG
void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func)
{
    asm volatile("cli");
    klog() << "ASSERTION FAILED: " << msg << "\n"
           << file << ":" << line << " in " << func;

    // Switch back to the current process's page tables if there are any.
    // Otherwise stack walking will be a disaster.
    if (Process::current)
        MM.enter_process_paging_scope(*Process::current);

    Kernel::dump_backtrace();
    asm volatile("hlt");
    for (;;)
        ;
}
#endif
