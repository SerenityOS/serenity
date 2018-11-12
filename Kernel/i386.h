#pragma once

#include "types.h"
#include "kprintf.h"

#define PAGE_SIZE 4096
#define PAGE_MASK 0xfffff000

union Descriptor {
    struct {
        WORD limit_lo;
        WORD base_lo;
        BYTE base_hi;
        BYTE type : 4;
        BYTE descriptor_type : 1;
        BYTE dpl : 2;
        BYTE segment_present : 1;
        BYTE limit_hi : 4;
        BYTE : 1;
        BYTE zero : 1;
        BYTE operation_size : 1;
        BYTE granularity : 1;
        BYTE base_hi2;
    };
    struct {
        DWORD low;
        DWORD high;
    };

    enum Type {
        Invalid = 0,
        AvailableTSS_16bit = 0x1,
        LDT = 0x2,
        BusyTSS_16bit = 0x3,
        CallGate_16bit = 0x4,
        TaskGate = 0x5,
        InterruptGate_16bit = 0x6,
        TrapGate_16bit = 0x7,
        AvailableTSS_32bit = 0x9,
        BusyTSS_32bit = 0xb,
        CallGate_32bit = 0xc,
        InterruptGate_32bit = 0xe,
        TrapGate_32bit = 0xf,
    };

    void setBase(void* b)
    {
        base_lo = (DWORD)(b) & 0xffff;
        base_hi = ((DWORD)(b) >> 16) & 0xff;
        base_hi2 = ((DWORD)(b) >> 24) & 0xff;
    }

    void setLimit(DWORD l)
    {
        limit_lo = (DWORD)l & 0xffff;
        limit_hi = ((DWORD)l >> 16) & 0xff;
    }
} PACKED;

class IRQHandler;

void gdt_init();
void idt_init();
void registerInterruptHandler(BYTE number, void (*f)());
void registerUserCallableInterruptHandler(BYTE number, void (*f)());
void registerIRQHandler(BYTE number, IRQHandler&);
void unregisterIRQHandler(BYTE number, IRQHandler&);
void flushIDT();
void flushGDT();
void load_task_register(WORD selector);
word gdt_alloc_entry();
void gdt_free_entry(word);
Descriptor& getGDTEntry(WORD selector);
void writeGDTEntry(WORD selector, Descriptor&);

#define HANG asm volatile( "cli; hlt" );
#define LSW(x) ((DWORD)(x) & 0xFFFF)
#define MSW(x) (((DWORD)(x) >> 16) & 0xFFFF)
#define LSB(x) ((x) & 0xFF)
#define MSB(x) (((x)>>8) & 0xFF)

#define cli() asm volatile("cli")
#define sti() asm volatile("sti")

static inline dword cpuFlags()
{
    dword flags;
    asm volatile(
        "pushf\n"
        "pop %0\n"
        :"=rm"(flags)
        ::"memory");
    return flags;
}

inline bool are_interrupts_enabled()
{
    return cpuFlags() & 0x200;
}

class InterruptDisabler {
public:
    InterruptDisabler()
    {
        m_flags = cpuFlags();
        cli();
    }

    ~InterruptDisabler()
    {
        if (m_flags & 0x200)
            sti();
    }

private:
    dword m_flags;
};

/* Map IRQ0-15 @ ISR 0x50-0x5F */
#define IRQ_VECTOR_BASE 0x50

struct PageFaultFlags {
enum Flags {
    NotPresent = 0x00,
    ProtectionViolation = 0x01,
    Read = 0x00,
    Write = 0x02,
    UserMode = 0x04,
    SupervisorMode = 0x00,
    InstructionFetch = 0x08,
};
};

class PageFault {
public:
    PageFault(word code, LinearAddress laddr)
        : m_code(code)
        , m_laddr(laddr)
    {
    }

    LinearAddress laddr() const { return m_laddr; }
    word code() const { return m_code; }

    bool is_not_present() const { return (m_code & 1) == PageFaultFlags::NotPresent; }
    bool is_protection_violation() const { return (m_code & 1) == PageFaultFlags::ProtectionViolation; }
    bool is_read() const { return (m_code & 2) == PageFaultFlags::Read; }
    bool is_write() const { return (m_code & 2) == PageFaultFlags::Write; }
    bool is_user() const { return (m_code & 4) == PageFaultFlags::UserMode; }
    bool is_supervisor() const { return (m_code & 4) == PageFaultFlags::SupervisorMode; }
    bool is_instruction_fetch() const { return (m_code & 8) == PageFaultFlags::InstructionFetch; }

private:
    word m_code;
    LinearAddress m_laddr;
};

struct RegisterDump {
    WORD ss;
    WORD gs;
    WORD fs;
    WORD es;
    WORD ds;
    DWORD edi;
    DWORD esi;
    DWORD ebp;
    DWORD esp;
    DWORD ebx;
    DWORD edx;
    DWORD ecx;
    DWORD eax;
    DWORD eip;
    WORD cs;
    WORD __csPadding;
    DWORD eflags;
    DWORD esp_if_crossRing;
    WORD ss_if_crossRing;
} PACKED;

struct RegisterDumpWithExceptionCode {
    WORD ss;
    WORD gs;
    WORD fs;
    WORD es;
    WORD ds;
    DWORD edi;
    DWORD esi;
    DWORD ebp;
    DWORD esp;
    DWORD ebx;
    DWORD edx;
    DWORD ecx;
    DWORD eax;
    WORD exception_code;
    WORD __exception_code_padding;
    DWORD eip;
    WORD cs;
    WORD __csPadding;
    DWORD eflags;
    DWORD esp_if_crossRing;
    WORD ss_if_crossRing;
} PACKED;

inline constexpr dword pageBaseOf(dword address)
{
    return address & 0xfffff000;
}

class CPUID {
public:
    CPUID(dword function) { asm volatile("cpuid" : "=a" (m_eax), "=b" (m_ebx), "=c" (m_ecx), "=d" (m_edx) : "a" (function), "c" (0)); }
    dword eax() const { return m_eax; }
    dword ebx() const { return m_ebx; }
    dword ecx() const { return m_ecx; }
    dword edx() const { return m_edx; }
private:
    dword m_eax { 0xffffffff };
    dword m_ebx { 0xffffffff };
    dword m_ecx { 0xffffffff };
    dword m_edx { 0xffffffff };
};

inline void read_tsc(dword& lsw, dword& msw)
{
    asm volatile("rdtsc":"=d"(msw),"=a"(lsw));
}

struct Stopwatch {
public:
    Stopwatch(const char* name)
        : m_name(name)
    {
        read_tsc(m_start_lsw, m_start_msw);
    }

    ~Stopwatch()
    {
        dword end_lsw;
        dword end_msw;
        read_tsc(end_lsw, end_msw);
        if (m_start_msw != end_msw) {
            dbgprintf("differing msw's\n");
            asm volatile("cli;hlt");
        }
        dword diff = end_lsw - m_start_lsw;
        dbgprintf("Stopwatch(%s): %u ticks\n", m_name, diff);
    }

private:
    const char* m_name { nullptr };
    dword m_start_lsw { 0 };
    dword m_start_msw { 0 };
};
