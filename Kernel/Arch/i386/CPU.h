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

#pragma once

#include <AK/Badge.h>
#include <AK/Noncopyable.h>
#include <LibBareMetal/Memory/PhysicalAddress.h>
#include <LibBareMetal/Memory/VirtualAddress.h>

#define PAGE_SIZE 4096
#define GENERIC_INTERRUPT_HANDLERS_COUNT 128
#define PAGE_MASK ((uintptr_t)0xfffff000u)

namespace Kernel {

class MemoryManager;
class PageDirectory;
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
        limit_hi = ((u32)l >> 16) & 0xf;
    }
};

class PageDirectoryEntry {
public:
    const PageTableEntry* page_table_base() const { return reinterpret_cast<PageTableEntry*>(m_raw & 0xfffff000u); }
    PageTableEntry* page_table_base() { return reinterpret_cast<PageTableEntry*>(m_raw & 0xfffff000u); }
    void set_page_table_base(u32 value)
    {
        m_raw &= 0x8000000000000fffULL;
        m_raw |= value & 0xfffff000;
    }

    void clear() { m_raw = 0; }

    u64 raw() const { return m_raw; }
    void copy_from(Badge<PageDirectory>, const PageDirectoryEntry& other) { m_raw = other.m_raw; }

    enum Flags {
        Present = 1 << 0,
        ReadWrite = 1 << 1,
        UserSupervisor = 1 << 2,
        WriteThrough = 1 << 3,
        CacheDisabled = 1 << 4,
        Huge = 1 << 7,
        Global = 1 << 8,
        NoExecute = 0x8000000000000000ULL,
    };

    bool is_present() const { return raw() & Present; }
    void set_present(bool b) { set_bit(Present, b); }

    bool is_user_allowed() const { return raw() & UserSupervisor; }
    void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

    bool is_huge() const { return raw() & Huge; }
    void set_huge(bool b) { set_bit(Huge, b); }

    bool is_writable() const { return raw() & ReadWrite; }
    void set_writable(bool b) { set_bit(ReadWrite, b); }

    bool is_write_through() const { return raw() & WriteThrough; }
    void set_write_through(bool b) { set_bit(WriteThrough, b); }

    bool is_cache_disabled() const { return raw() & CacheDisabled; }
    void set_cache_disabled(bool b) { set_bit(CacheDisabled, b); }

    bool is_global() const { return raw() & Global; }
    void set_global(bool b) { set_bit(Global, b); }

    bool is_execute_disabled() const { return raw() & NoExecute; }
    void set_execute_disabled(bool b) { set_bit(NoExecute, b); }

    void set_bit(u64 bit, bool value)
    {
        if (value)
            m_raw |= bit;
        else
            m_raw &= ~bit;
    }

private:
    u64 m_raw;
};

class PageTableEntry {
public:
    void* physical_page_base() { return reinterpret_cast<void*>(m_raw & 0xfffff000u); }
    void set_physical_page_base(u32 value)
    {
        m_raw &= 0x8000000000000fffULL;
        m_raw |= value & 0xfffff000;
    }

    u64 raw() const { return (u32)m_raw; }

    enum Flags {
        Present = 1 << 0,
        ReadWrite = 1 << 1,
        UserSupervisor = 1 << 2,
        WriteThrough = 1 << 3,
        CacheDisabled = 1 << 4,
        Global = 1 << 8,
        NoExecute = 0x8000000000000000ULL,
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

    bool is_global() const { return raw() & Global; }
    void set_global(bool b) { set_bit(Global, b); }

    bool is_execute_disabled() const { return raw() & NoExecute; }
    void set_execute_disabled(bool b) { set_bit(NoExecute, b); }

    void clear() { m_raw = 0; }

    void set_bit(u64 bit, bool value)
    {
        if (value)
            m_raw |= bit;
        else
            m_raw &= ~bit;
    }

private:
    u64 m_raw;
};

static_assert(sizeof(PageDirectoryEntry) == 8);
static_assert(sizeof(PageTableEntry) == 8);

class PageDirectoryPointerTable {
public:
    PageDirectoryEntry* directory(size_t index)
    {
        return (PageDirectoryEntry*)(raw[index] & ~0xfffu);
    }

    u64 raw[4];
};

class GenericInterruptHandler;
struct RegisterState;

void gdt_init();
void idt_init();
void sse_init();
void register_interrupt_handler(u8 number, void (*f)());
void register_user_callable_interrupt_handler(u8 number, void (*f)());
GenericInterruptHandler& get_interrupt_handler(u8 interrupt_number);
void register_generic_interrupt_handler(u8 number, GenericInterruptHandler&);
void replace_single_handler_with_shared(GenericInterruptHandler&);
void replace_shared_handler_with_single(GenericInterruptHandler&);
void unregister_generic_interrupt_handler(u8 number, GenericInterruptHandler&);
void flush_idt();
void flush_gdt();
void load_task_register(u16 selector);
u16 gdt_alloc_entry();
void gdt_free_entry(u16);
Descriptor& get_gdt_entry(u16 selector);
void write_gdt_entry(u16 selector, Descriptor&);
void handle_crash(RegisterState&, const char* description, int signal);

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
inline u32 cpu_flags()
{
    u32 flags;
    asm volatile(
        "pushf\n"
        "pop %0\n"
        : "=rm"(flags)::"memory");
    return flags;
}

inline u32 read_fs_u32(u32 offset)
{
    u32 val;
    asm volatile(
        "movl %%fs:%a[off], %k[val]"
        : [ val ] "=r"(val)
        : [ off ] "ir"(offset));
    return val;
}

inline void write_fs_u32(u32 offset, u32 val)
{
    asm volatile(
        "movl %k[val], %%fs:%a[off]" ::[off] "ir"(offset), [ val ] "ir"(val)
        : "memory");
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

inline bool cli_and_save_interrupt_flag()
{
    u32 flags = cpu_flags();
    cli();
    return flags & 0x200;
}

inline void restore_interrupt_flag(bool flag)
{
    if (flag)
        sti();
    else
        cli();
}

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
        ReservedBitViolation = 0x08,
        InstructionFetch = 0x10,
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

struct [[gnu::packed]] RegisterState
{
    u32 ss;
    u32 gs;
    u32 fs;
    u32 es;
    u32 ds;
    u32 edi;
    u32 esi;
    u32 ebp;
    u32 esp;
    u32 ebx;
    u32 edx;
    u32 ecx;
    u32 eax;
    u16 exception_code;
    u16 isr_number;
    u32 eip;
    u32 cs;
    u32 eflags;
    u32 userspace_esp;
    u32 userspace_ss;
};

struct [[gnu::aligned(16)]] FPUState
{
    u8 buffer[512];
};

inline constexpr uintptr_t page_base_of(uintptr_t address)
{
    return address & PAGE_MASK;
}

inline uintptr_t page_base_of(const void* address)
{
    return page_base_of((uintptr_t)address);
}

inline constexpr uintptr_t offset_in_page(uintptr_t address)
{
    return address & (~PAGE_MASK);
}

inline uintptr_t offset_in_page(const void* address)
{
    return offset_in_page((uintptr_t)address);
}

u32 read_cr3();
void write_cr3(u32);

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

inline u64 read_tsc()
{
    u32 lsw;
    u32 msw;
    read_tsc(lsw, msw);
    return ((u64)msw << 32) | lsw;
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
        dbg() << "Stopwatch(" << m_name << "): " << diff << " ticks";
    }

private:
    const char* m_name { nullptr };
    SplitQword m_start;
};

class MSR {
    uint32_t m_msr;

public:
    static bool have()
    {
        CPUID id(1);
        return (id.edx() & (1 << 5)) != 0;
    }

    MSR(const MSR&) = delete;
    MSR& operator=(const MSR&) = delete;

    MSR(uint32_t msr)
        : m_msr(msr)
    {
    }

    void get(u32& low, u32& high)
    {
        asm volatile("rdmsr"
                     : "=a"(low), "=d"(high)
                     : "c"(m_msr));
    }

    void set(u32 low, u32 high)
    {
        asm volatile("wrmsr" ::"a"(low), "d"(high), "c"(m_msr));
    }
};

void cpu_setup();
extern bool g_cpu_supports_nx;
extern bool g_cpu_supports_pae;
extern bool g_cpu_supports_pge;
extern bool g_cpu_supports_rdrand;
extern bool g_cpu_supports_smap;
extern bool g_cpu_supports_smep;
extern bool g_cpu_supports_sse;
extern bool g_cpu_supports_tsc;
extern bool g_cpu_supports_umip;

void stac();
void clac();

class SmapDisabler {
public:
    SmapDisabler()
    {
        m_flags = cpu_flags();
        stac();
    }

    ~SmapDisabler()
    {
        if (!(m_flags & 0x40000))
            clac();
    }

private:
    u32 m_flags;
};

extern u32 g_in_irq;

}
