#pragma once

#include <AK/Badge.h>
#include <AK/Noncopyable.h>
#include <Kernel/VM/VirtualAddress.h>
#include <Kernel/kstdio.h>

#define PAGE_SIZE 4096
#define PAGE_MASK 0xfffff000

class MemoryManager;
class PageTableEntry;

struct [[gnu::packed]] TSS32
{
    u16 backlink, __blh;
    u32 esp0;
    u16 ss0, __ss0h;
    u32 esp1;
    u16 ss1, __ss1h;
    u32 esp2;
    u16 ss2, __ss2h;
    u32 cr3, eip, eflags;
    u32 eax, ecx, edx, ebx, esp, ebp, esi, edi;
    u16 es, __esh;
    u16 cs, __csh;
    u16 ss, __ssh;
    u16 ds, __dsh;
    u16 fs, __fsh;
    u16 gs, __gsh;
    u16 ldt, __ldth;
    u16 trace, iomapbase;
};

union [[gnu::packed]] Descriptor
{
    struct {
        u16 limit_lo;
        u16 base_lo;
        u8 base_hi;
        u8 type : 4;
        u8 descriptor_type : 1;
        u8 dpl : 2;
        u8 segment_present : 1;
        u8 limit_hi : 4;
        u8 : 1;
        u8 zero : 1;
        u8 operation_size : 1;
        u8 granularity : 1;
        u8 base_hi2;
    };
    struct {
        u32 low;
        u32 high;
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

    void set_base(void* b)
    {
        base_lo = (u32)(b)&0xffff;
        base_hi = ((u32)(b) >> 16) & 0xff;
        base_hi2 = ((u32)(b) >> 24) & 0xff;
    }

    void set_limit(u32 l)
    {
        limit_lo = (u32)l & 0xffff;
        limit_hi = ((u32)l >> 16) & 0xff;
    }
};

class PageDirectoryEntry {
    AK_MAKE_NONCOPYABLE(PageDirectoryEntry);

public:
    PageTableEntry* page_table_base() { return reinterpret_cast<PageTableEntry*>(m_raw & 0xfffff000u); }
    void set_page_table_base(u32 value)
    {
        m_raw &= 0xfff;
        m_raw |= value & 0xfffff000;
    }

    u32 raw() const { return m_raw; }
    void copy_from(Badge<MemoryManager>, const PageDirectoryEntry& other) { m_raw = other.m_raw; }

    enum Flags {
        Present = 1 << 0,
        ReadWrite = 1 << 1,
        UserSupervisor = 1 << 2,
        WriteThrough = 1 << 3,
        CacheDisabled = 1 << 4,
    };

    bool is_present() const { return raw() & Present; }
    void set_present(bool b) { set_bit(Present, b); }

    bool is_user_allowed() const { return raw() & UserSupervisor; }
    void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

    bool is_writable() const { return raw() & ReadWrite; }
    void set_writable(bool b) { set_bit(ReadWrite, b); }

    bool is_write_through() const { return raw() & WriteThrough; }
    void set_write_through(bool b) { set_bit(WriteThrough, b); }

    bool is_cache_disabled() const { return raw() & CacheDisabled; }
    void set_cache_disabled(bool b) { set_bit(CacheDisabled, b); }

    void set_bit(u8 bit, bool value)
    {
        if (value)
            m_raw |= bit;
        else
            m_raw &= ~bit;
    }

private:
    u32 m_raw;
};

class PageTableEntry {
    AK_MAKE_NONCOPYABLE(PageTableEntry);

public:
    void* physical_page_base() { return reinterpret_cast<void*>(m_raw & 0xfffff000u); }
    void set_physical_page_base(u32 value)
    {
        m_raw &= 0xfff;
        m_raw |= value & 0xfffff000;
    }

    u32 raw() const { return m_raw; }

    enum Flags {
        Present = 1 << 0,
        ReadWrite = 1 << 1,
        UserSupervisor = 1 << 2,
        WriteThrough = 1 << 3,
        CacheDisabled = 1 << 4,
    };

    bool is_present() const { return raw() & Present; }
    void set_present(bool b) { set_bit(Present, b); }

    bool is_user_allowed() const { return raw() & UserSupervisor; }
    void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

    bool is_writable() const { return raw() & ReadWrite; }
    void set_writable(bool b) { set_bit(ReadWrite, b); }

    bool is_write_through() const { return raw() & WriteThrough; }
    void set_write_through(bool b) { set_bit(WriteThrough, b); }

    bool is_cache_disabled() const { return raw() & CacheDisabled; }
    void set_cache_disabled(bool b) { set_bit(CacheDisabled, b); }

    void set_bit(u8 bit, bool value)
    {
        if (value)
            m_raw |= bit;
        else
            m_raw &= ~bit;
    }

private:
    u32 m_raw;
};

static_assert(sizeof(PageDirectoryEntry) == 4);
static_assert(sizeof(PageTableEntry) == 4);

class IRQHandler;

void gdt_init();
void idt_init();
void sse_init();
void register_interrupt_handler(u8 number, void (*f)());
void register_user_callable_interrupt_handler(u8 number, void (*f)());
void register_irq_handler(u8 number, IRQHandler&);
void unregister_irq_handler(u8 number, IRQHandler&);
void flush_idt();
void flush_gdt();
void load_task_register(u16 selector);
u16 gdt_alloc_entry();
void gdt_free_entry(u16);
Descriptor& get_gdt_entry(u16 selector);
void write_gdt_entry(u16 selector, Descriptor&);

[[noreturn]] static inline void hang()
{
    asm volatile("cli; hlt");
    for (;;) {
    }
}

#define LSW(x) ((u32)(x)&0xFFFF)
#define MSW(x) (((u32)(x) >> 16) & 0xFFFF)
#define LSB(x) ((x)&0xFF)
#define MSB(x) (((x) >> 8) & 0xFF)

#define cli() asm volatile("cli" :: \
                               : "memory")
#define sti() asm volatile("sti" :: \
                               : "memory")
#define memory_barrier() asm volatile("" :: \
                                          : "memory")

inline u32 cpu_cr3()
{
    u32 cr3;
    asm volatile("movl %%cr3, %%eax"
                 : "=a"(cr3));
    return cr3;
}

inline u32 cpu_flags()
{
    u32 flags;
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
    u32 m_flags;
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
    u32 m_flags;
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
    PageFault(u16 code, VirtualAddress vaddr)
        : m_code(code)
        , m_vaddr(vaddr)
    {
    }

    enum class Type {
        PageNotPresent = PageFaultFlags::NotPresent,
        ProtectionViolation = PageFaultFlags::ProtectionViolation,
    };

    enum class Access {
        Read = PageFaultFlags::Read,
        Write = PageFaultFlags::Write,
    };

    VirtualAddress vaddr() const { return m_vaddr; }
    u16 code() const { return m_code; }

    Type type() const { return (Type)(m_code & 1); }
    Access access() const { return (Access)(m_code & 2); }

    bool is_not_present() const { return (m_code & 1) == PageFaultFlags::NotPresent; }
    bool is_protection_violation() const { return (m_code & 1) == PageFaultFlags::ProtectionViolation; }
    bool is_read() const { return (m_code & 2) == PageFaultFlags::Read; }
    bool is_write() const { return (m_code & 2) == PageFaultFlags::Write; }
    bool is_user() const { return (m_code & 4) == PageFaultFlags::UserMode; }
    bool is_supervisor() const { return (m_code & 4) == PageFaultFlags::SupervisorMode; }
    bool is_instruction_fetch() const { return (m_code & 8) == PageFaultFlags::InstructionFetch; }

private:
    u16 m_code;
    VirtualAddress m_vaddr;
};

struct [[gnu::packed]] RegisterDump
{
    u16 ss;
    u16 gs;
    u16 fs;
    u16 es;
    u16 ds;
    u32 edi;
    u32 esi;
    u32 ebp;
    u32 esp;
    u32 ebx;
    u32 edx;
    u32 ecx;
    u32 eax;
    u16 exception_code;
    u16 __exception_code_padding;
    u32 eip;
    u16 cs;
    u16 __csPadding;
    u32 eflags;
    u32 esp_if_crossRing;
    u16 ss_if_crossRing;
};


struct [[gnu::aligned(16)]] FPUState
{
    u8 buffer[512];
};

inline constexpr u32 page_base_of(u32 address)
{
    return address & 0xfffff000;
}

class CPUID {
public:
    CPUID(u32 function) { asm volatile("cpuid"
                                         : "=a"(m_eax), "=b"(m_ebx), "=c"(m_ecx), "=d"(m_edx)
                                         : "a"(function), "c"(0)); }
    u32 eax() const { return m_eax; }
    u32 ebx() const { return m_ebx; }
    u32 ecx() const { return m_ecx; }
    u32 edx() const { return m_edx; }

private:
    u32 m_eax { 0xffffffff };
    u32 m_ebx { 0xffffffff };
    u32 m_ecx { 0xffffffff };
    u32 m_edx { 0xffffffff };
};

inline void read_tsc(u32& lsw, u32& msw)
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
