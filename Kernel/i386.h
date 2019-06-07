#pragma once

#include <Kernel/VirtualAddress.h>
#include <Kernel/kstdio.h>

#define PAGE_SIZE 4096
#define PAGE_MASK 0xfffff000

struct [[gnu::packed]] TSS32
{
    word backlink, __blh;
    dword esp0;
    word ss0, __ss0h;
    dword esp1;
    word ss1, __ss1h;
    dword esp2;
    word ss2, __ss2h;
    dword cr3, eip, eflags;
    dword eax, ecx, edx, ebx, esp, ebp, esi, edi;
    word es, __esh;
    word cs, __csh;
    word ss, __ssh;
    word ds, __dsh;
    word fs, __fsh;
    word gs, __gsh;
    word ldt, __ldth;
    word trace, iomapbase;
};

union [[gnu::packed]] Descriptor
{
    struct {
        word limit_lo;
        word base_lo;
        byte base_hi;
        byte type : 4;
        byte descriptor_type : 1;
        byte dpl : 2;
        byte segment_present : 1;
        byte limit_hi : 4;
        byte : 1;
        byte zero : 1;
        byte operation_size : 1;
        byte granularity : 1;
        byte base_hi2;
    };
    struct {
        dword low;
        dword high;
    };

    enum Type
    {
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

    void set_base(void* b)
    {
        base_lo = (dword)(b)&0xffff;
        base_hi = ((dword)(b) >> 16) & 0xff;
        base_hi2 = ((dword)(b) >> 24) & 0xff;
    }

    void set_limit(dword l)
    {
        limit_lo = (dword)l & 0xffff;
        limit_hi = ((dword)l >> 16) & 0xff;
    }
};

class IRQHandler;

void gdt_init();
void idt_init();
void sse_init();
void register_interrupt_handler(byte number, void (*f)());
void register_user_callable_interrupt_handler(byte number, void (*f)());
void register_irq_handler(byte number, IRQHandler&);
void unregister_irq_handler(byte number, IRQHandler&);
void flush_idt();
void flush_gdt();
void load_task_register(word selector);
word gdt_alloc_entry();
void gdt_free_entry(word);
Descriptor& get_gdt_entry(word selector);
void write_gdt_entry(word selector, Descriptor&);

[[noreturn]] static inline void hang()
{
    asm volatile("cli; hlt");
    for (;;) {
    }
}

#define LSW(x) ((dword)(x)&0xFFFF)
#define MSW(x) (((dword)(x) >> 16) & 0xFFFF)
#define LSB(x) ((x)&0xFF)
#define MSB(x) (((x) >> 8) & 0xFF)

#define cli() asm volatile("cli" :: \
                               : "memory")
#define sti() asm volatile("sti" :: \
                               : "memory")
#define memory_barrier() asm volatile("" :: \
                                          : "memory")

inline dword cpu_cr3()
{
    dword cr3;
    asm volatile("movl %%cr3, %%eax"
                 : "=a"(cr3));
    return cr3;
}

inline dword cpu_flags()
{
    dword flags;
    asm volatile(
        "pushf\n"
        "pop %0\n"
        : "=rm"(flags)::"memory");
    return flags;
}

inline bool are_interrupts_enabled()
{
    return cpu_flags() & 0x200;
}

class InterruptFlagSaver {
public:
    InterruptFlagSaver()
    {
        m_flags = cpu_flags();
    }

    ~InterruptFlagSaver()
    {
        if (m_flags & 0x200)
            sti();
        else
            cli();
    }

private:
    dword m_flags;
};

class InterruptDisabler {
public:
    InterruptDisabler()
    {
        m_flags = cpu_flags();
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
    enum Flags
    {
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
    PageFault(word code, VirtualAddress vaddr)
        : m_code(code)
        , m_vaddr(vaddr)
    {
    }

    VirtualAddress vaddr() const { return m_vaddr; }
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
    VirtualAddress m_vaddr;
};

struct [[gnu::packed]] RegisterDump
{
    word ss;
    word gs;
    word fs;
    word es;
    word ds;
    dword edi;
    dword esi;
    dword ebp;
    dword esp;
    dword ebx;
    dword edx;
    dword ecx;
    dword eax;
    dword eip;
    word cs;
    word __csPadding;
    dword eflags;
    dword esp_if_crossRing;
    word ss_if_crossRing;
};

struct [[gnu::packed]] RegisterDumpWithExceptionCode
{
    word ss;
    word gs;
    word fs;
    word es;
    word ds;
    dword edi;
    dword esi;
    dword ebp;
    dword esp;
    dword ebx;
    dword edx;
    dword ecx;
    dword eax;
    word exception_code;
    word __exception_code_padding;
    dword eip;
    word cs;
    word __csPadding;
    dword eflags;
    dword esp_if_crossRing;
    word ss_if_crossRing;
};

struct [[gnu::aligned(16)]] FPUState
{
    byte buffer[512];
};

inline constexpr dword page_base_of(dword address)
{
    return address & 0xfffff000;
}

class CPUID {
public:
    CPUID(dword function) { asm volatile("cpuid"
                                         : "=a"(m_eax), "=b"(m_ebx), "=c"(m_ecx), "=d"(m_edx)
                                         : "a"(function), "c"(0)); }
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
    asm volatile("rdtsc"
                 : "=d"(msw), "=a"(lsw));
}

struct Stopwatch {
    union SplitQword {
        struct {
            uint32_t lsw;
            uint32_t msw;
        };
        uint64_t qw { 0 };
    };

public:
    Stopwatch(const char* name)
        : m_name(name)
    {
        read_tsc(m_start.lsw, m_start.msw);
    }

    ~Stopwatch()
    {
        SplitQword end;
        read_tsc(end.lsw, end.msw);
        uint64_t diff = end.qw - m_start.qw;
        dbgprintf("Stopwatch(%s): %Q ticks\n", m_name, diff);
    }

private:
    const char* m_name { nullptr };
    SplitQword m_start;
};
