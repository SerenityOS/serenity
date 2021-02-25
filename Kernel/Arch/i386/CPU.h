/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/Atomic.h>
#include <AK/Badge.h>
#include <AK/Noncopyable.h>
#include <AK/Vector.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VirtualAddress.h>
#include <LibC/sys/arch/i386/regs.h>

#define READONLY_AFTER_INIT __attribute__((section(".ro_after_init")))
#define UNMAP_AFTER_INIT NEVER_INLINE __attribute__((section(".unmap_after_init")))

#define PAGE_SIZE 4096
#define GENERIC_INTERRUPT_HANDLERS_COUNT (256 - IRQ_VECTOR_BASE)
#define PAGE_MASK ((FlatPtr)0xfffff000u)

namespace Kernel {

class MemoryManager;
class PageDirectory;
class PageTableEntry;

static constexpr u32 safe_eflags_mask = 0xdff;
static constexpr u32 iopl_mask = 3u << 12;

inline u32 get_iopl_from_eflags(u32 eflags)
{
    return (eflags & iopl_mask) >> 12;
}

struct [[gnu::packed]] DescriptorTablePointer {
    u16 limit;
    void* address;
};

struct [[gnu::packed]] TSS32 {
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

union [[gnu::packed]] Descriptor {
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

    VirtualAddress base() const
    {
        FlatPtr base = base_lo;
        base |= base_hi << 16u;
        base |= base_hi2 << 24u;
        return VirtualAddress { base };
    }

    void set_base(VirtualAddress base)
    {
        base_lo = base.get() & 0xffffu;
        base_hi = (base.get() >> 16u) & 0xffu;
        base_hi2 = (base.get() >> 24u) & 0xffu;
    }

    void set_limit(u32 length)
    {
        limit_lo = length & 0xffff;
        limit_hi = (length >> 16) & 0xf;
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

    bool is_null() const { return m_raw == 0; }
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

    bool is_null() const { return m_raw == 0; }
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

const DescriptorTablePointer& get_gdtr();
const DescriptorTablePointer& get_idtr();
void register_interrupt_handler(u8 number, void (*handler)());
void register_user_callable_interrupt_handler(u8 number, void (*handler)());
GenericInterruptHandler& get_interrupt_handler(u8 interrupt_number);
void register_generic_interrupt_handler(u8 number, GenericInterruptHandler&);
void unregister_generic_interrupt_handler(u8 number, GenericInterruptHandler&);
void flush_idt();
void load_task_register(u16 selector);
[[noreturn]] void handle_crash(RegisterState&, const char* description, int signal, bool out_of_memory = false);

#define LSW(x) ((u32)(x)&0xFFFF)
#define MSW(x) (((u32)(x) >> 16) & 0xFFFF)
#define LSB(x) ((x)&0xFF)
#define MSB(x) (((x) >> 8) & 0xFF)

#define cli() asm volatile("cli" :: \
                               : "memory")
#define sti() asm volatile("sti" :: \
                               : "memory")

inline FlatPtr cpu_flags()
{
    FlatPtr flags;
    asm volatile(
        "pushf\n"
        "pop %0\n"
        : "=rm"(flags)::"memory");
    return flags;
}

inline void set_fs(u16 segment)
{
    asm volatile(
        "mov %%ax, %%fs" ::"a"(segment)
        : "memory");
}

inline void set_gs(u16 segment)
{
    asm volatile(
        "mov %%ax, %%gs" ::"a"(segment)
        : "memory");
}

inline u16 get_fs()
{
    u16 fs;
    asm("mov %%fs, %%eax"
        : "=a"(fs));
    return fs;
}

inline u16 get_gs()
{
    u16 gs;
    asm("mov %%gs, %%eax"
        : "=a"(gs));
    return gs;
}

inline u32 read_fs_u32(u32 offset)
{
    u32 val;
    asm volatile(
        "movl %%fs:%a[off], %k[val]"
        : [val] "=r"(val)
        : [off] "ir"(offset));
    return val;
}

inline FlatPtr read_fs_ptr(u32 offset)
{
    return read_fs_u32(offset);
}

inline void write_fs_u32(u32 offset, u32 val)
{
    asm volatile(
        "movl %k[val], %%fs:%a[off]" ::[off] "ir"(offset), [val] "ir"(val)
        : "memory");
}

inline bool are_interrupts_enabled()
{
    return cpu_flags() & 0x200;
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

class NonMaskableInterruptDisabler {
public:
    NonMaskableInterruptDisabler();
    ~NonMaskableInterruptDisabler();
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

struct [[gnu::packed]] RegisterState {
    FlatPtr ss;
    FlatPtr gs;
    FlatPtr fs;
    FlatPtr es;
    FlatPtr ds;
    FlatPtr edi;
    FlatPtr esi;
    FlatPtr ebp;
    FlatPtr esp;
    FlatPtr ebx;
    FlatPtr edx;
    FlatPtr ecx;
    FlatPtr eax;
    u16 exception_code;
    u16 isr_number;
#if ARCH(X86_64)
    u32 padding;
#endif
    FlatPtr eip;
    FlatPtr cs;
    FlatPtr eflags;
    FlatPtr userspace_esp;
    FlatPtr userspace_ss;
};

#if ARCH(I386)
#    define REGISTER_STATE_SIZE (19 * 4)
#else
#    define REGISTER_STATE_SIZE (19 * 8)
#endif
static_assert(REGISTER_STATE_SIZE == sizeof(RegisterState));

void copy_kernel_registers_into_ptrace_registers(PtraceRegisters&, const RegisterState&);
void copy_ptrace_registers_into_kernel_registers(RegisterState&, const PtraceRegisters&);

struct [[gnu::aligned(16)]] FPUState
{
    u8 buffer[512];
};

constexpr FlatPtr page_base_of(FlatPtr address)
{
    return address & PAGE_MASK;
}

inline FlatPtr page_base_of(const void* address)
{
    return page_base_of((FlatPtr)address);
}

constexpr FlatPtr offset_in_page(FlatPtr address)
{
    return address & (~PAGE_MASK);
}

inline FlatPtr offset_in_page(const void* address)
{
    return offset_in_page((FlatPtr)address);
}

FlatPtr read_cr0();
FlatPtr read_cr2();
FlatPtr read_cr3();
FlatPtr read_cr4();

void write_cr0(FlatPtr);
void write_cr3(FlatPtr);
void write_cr4(FlatPtr);

FlatPtr read_dr6();

static inline bool is_kernel_mode()
{
    u16 cs;
    asm volatile(
        "mov %%cs, %[cs] \n"
        : [cs] "=g"(cs));
    return (cs & 3) == 0;
}

class CPUID {
public:
    explicit CPUID(u32 function) { asm volatile("cpuid"
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

// FIXME: This can't hold every CPU feature as-is.
enum class CPUFeature : u32 {
    NX = (1 << 0),
    PAE = (1 << 1),
    PGE = (1 << 2),
    RDRAND = (1 << 3),
    RDSEED = (1 << 4),
    SMAP = (1 << 5),
    SMEP = (1 << 6),
    SSE = (1 << 7),
    TSC = (1 << 8),
    RDTSCP = (1 << 9),
    CONSTANT_TSC = (1 << 10),
    NONSTOP_TSC = (1 << 11),
    UMIP = (1 << 12),
    SEP = (1 << 13),
    SYSCALL = (1 << 14),
    MMX = (1 << 15),
    SSE2 = (1 << 16),
    SSE3 = (1 << 17),
    SSSE3 = (1 << 18),
    SSE4_1 = (1 << 19),
    SSE4_2 = (1 << 20),
};

class Thread;
struct TrapFrame;

#define GDT_SELECTOR_CODE0 0x08
#define GDT_SELECTOR_DATA0 0x10
#define GDT_SELECTOR_CODE3 0x18
#define GDT_SELECTOR_DATA3 0x20
#define GDT_SELECTOR_TLS 0x28
#define GDT_SELECTOR_PROC 0x30
#define GDT_SELECTOR_TSS 0x38

// SYSENTER makes certain assumptions on how the GDT is structured:
static_assert(GDT_SELECTOR_CODE0 + 8 == GDT_SELECTOR_DATA0); // SS0 = CS0 + 8

// SYSEXIT makes certain assumptions on how the GDT is structured:
static_assert(GDT_SELECTOR_CODE0 + 16 == GDT_SELECTOR_CODE3); // CS3 = CS0 + 16
static_assert(GDT_SELECTOR_CODE0 + 24 == GDT_SELECTOR_DATA3); // SS3 = CS0 + 32

class ProcessorInfo;
class SchedulerPerProcessorData;
struct MemoryManagerData;
struct ProcessorMessageEntry;

struct ProcessorMessage {
    enum Type {
        FlushTlb,
        Callback,
        CallbackWithData
    };
    Type type;
    volatile u32 refs; // atomic
    union {
        ProcessorMessage* next; // only valid while in the pool
        struct {
            void (*handler)();
        } callback;
        struct {
            void* data;
            void (*handler)(void*);
            void (*free)(void*);
        } callback_with_data;
        struct {
            const PageDirectory* page_directory;
            u8* ptr;
            size_t page_count;
        } flush_tlb;
    };

    volatile bool async;

    ProcessorMessageEntry* per_proc_entries;
};

struct ProcessorMessageEntry {
    ProcessorMessageEntry* next;
    ProcessorMessage* msg;
};

struct DeferredCallEntry {
    DeferredCallEntry* next;
    union {
        struct {
            void (*handler)();
        } callback;
        struct {
            void* data;
            void (*handler)(void*);
            void (*free)(void*);
        } callback_with_data;
    };
    bool have_data;
    bool was_allocated;
};

class Processor {
    friend class ProcessorInfo;

    AK_MAKE_NONCOPYABLE(Processor);
    AK_MAKE_NONMOVABLE(Processor);

    Processor* m_self;

    DescriptorTablePointer m_gdtr;
    Descriptor m_gdt[256];
    u32 m_gdt_length;

    u32 m_cpu;
    u32 m_in_irq;
    Atomic<u32, AK::MemoryOrder::memory_order_relaxed> m_in_critical;
    static Atomic<u32> s_idle_cpu_mask;

    TSS32 m_tss;
    static FPUState s_clean_fpu_state;
    CPUFeature m_features;
    static volatile u32 g_total_processors; // atomic
    u8 m_physical_address_bit_width;

    ProcessorInfo* m_info;
    MemoryManagerData* m_mm_data;
    SchedulerPerProcessorData* m_scheduler_data;
    Thread* m_current_thread;
    Thread* m_idle_thread;

    volatile ProcessorMessageEntry* m_message_queue; // atomic, LIFO

    bool m_invoke_scheduler_async;
    bool m_scheduler_initialized;
    Atomic<bool> m_halt_requested;

    DeferredCallEntry* m_pending_deferred_calls; // in reverse order
    DeferredCallEntry* m_free_deferred_call_pool_entry;
    DeferredCallEntry m_deferred_call_pool[5];

    void gdt_init();
    void write_raw_gdt_entry(u16 selector, u32 low, u32 high);
    void write_gdt_entry(u16 selector, Descriptor& descriptor);
    static Vector<Processor*>& processors();

    static void smp_return_to_pool(ProcessorMessage& msg);
    static ProcessorMessage& smp_get_from_pool();
    static void smp_cleanup_message(ProcessorMessage& msg);
    bool smp_queue_message(ProcessorMessage& msg);
    static void smp_unicast_message(u32 cpu, ProcessorMessage& msg, bool async);
    static void smp_broadcast_message(ProcessorMessage& msg);
    static void smp_broadcast_wait_sync(ProcessorMessage& msg);
    static void smp_broadcast_halt();

    void deferred_call_pool_init();
    void deferred_call_execute_pending();
    DeferredCallEntry* deferred_call_get_free();
    void deferred_call_return_to_pool(DeferredCallEntry*);
    void deferred_call_queue_entry(DeferredCallEntry*);

    void cpu_detect();
    void cpu_setup();

    String features_string() const;

public:
    Processor() = default;

    void early_initialize(u32 cpu);
    void initialize(u32 cpu);

    void idle_begin()
    {
        s_idle_cpu_mask.fetch_or(1u << m_cpu, AK::MemoryOrder::memory_order_relaxed);
    }

    void idle_end()
    {
        s_idle_cpu_mask.fetch_and(~(1u << m_cpu), AK::MemoryOrder::memory_order_relaxed);
    }

    static u32 count()
    {
        // NOTE: because this value never changes once all APs are booted,
        // we don't really need to do an atomic_load() on this variable
        return g_total_processors;
    }

    ALWAYS_INLINE static void wait_check()
    {
        Processor::current().smp_process_pending_messages();
        // TODO: pause
    }

    [[noreturn]] static void halt();

    static void flush_entire_tlb_local()
    {
        write_cr3(read_cr3());
    }

    static void flush_tlb_local(VirtualAddress vaddr, size_t page_count);
    static void flush_tlb(const PageDirectory*, VirtualAddress, size_t);

    Descriptor& get_gdt_entry(u16 selector);
    void flush_gdt();
    const DescriptorTablePointer& get_gdtr();

    static Processor& by_id(u32 cpu);

    static size_t processor_count() { return processors().size(); }

    template<typename Callback>
    static inline IterationDecision for_each(Callback callback)
    {
        auto& procs = processors();
        size_t count = procs.size();
        for (size_t i = 0; i < count; i++) {
            if (callback(*procs[i]) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    ALWAYS_INLINE u8 physical_address_bit_width() const { return m_physical_address_bit_width; }

    ALWAYS_INLINE ProcessorInfo& info() { return *m_info; }

    ALWAYS_INLINE static Processor& current()
    {
        return *(Processor*)read_fs_ptr(__builtin_offsetof(Processor, m_self));
    }

    ALWAYS_INLINE static bool is_initialized()
    {
        return get_fs() == GDT_SELECTOR_PROC && read_fs_u32(__builtin_offsetof(Processor, m_self)) != 0;
    }

    ALWAYS_INLINE void set_scheduler_data(SchedulerPerProcessorData& scheduler_data)
    {
        m_scheduler_data = &scheduler_data;
    }

    ALWAYS_INLINE SchedulerPerProcessorData& get_scheduler_data() const
    {
        return *m_scheduler_data;
    }

    ALWAYS_INLINE void set_mm_data(MemoryManagerData& mm_data)
    {
        m_mm_data = &mm_data;
    }

    ALWAYS_INLINE MemoryManagerData& get_mm_data() const
    {
        return *m_mm_data;
    }

    ALWAYS_INLINE Thread* idle_thread() const
    {
        return m_idle_thread;
    }

    ALWAYS_INLINE void set_idle_thread(Thread& idle_thread)
    {
        m_idle_thread = &idle_thread;
    }

    ALWAYS_INLINE static Thread* current_thread()
    {
        // If we were to use Processor::current here, we'd have to
        // disable interrupts to prevent a race where we may get pre-empted
        // right after getting the Processor structure and then get moved
        // to another processor, which would lead us to get the wrong thread.
        // To avoid having to disable interrupts, we can just read the field
        // directly in an atomic fashion, similar to Processor::current.
        return (Thread*)read_fs_ptr(__builtin_offsetof(Processor, m_current_thread));
    }

    ALWAYS_INLINE static void set_current_thread(Thread& current_thread)
    {
        // See comment in Processor::current_thread
        write_fs_u32(__builtin_offsetof(Processor, m_current_thread), FlatPtr(&current_thread));
    }

    ALWAYS_INLINE u32 get_id() const
    {
        // NOTE: This variant should only be used when iterating over all
        // Processor instances, or when it's guaranteed that the thread
        // cannot move to another processor in between calling Processor::current
        // and Processor::get_id, or if this fact is not important.
        // All other cases should use Processor::id instead!
        return m_cpu;
    }

    ALWAYS_INLINE static u32 id()
    {
        // See comment in Processor::current_thread
        return read_fs_ptr(__builtin_offsetof(Processor, m_cpu));
    }

    ALWAYS_INLINE u32 raise_irq()
    {
        return m_in_irq++;
    }

    ALWAYS_INLINE void restore_irq(u32 prev_irq)
    {
        VERIFY(prev_irq <= m_in_irq);
        if (!prev_irq) {
            u32 prev_critical = 0;
            if (m_in_critical.compare_exchange_strong(prev_critical, 1)) {
                m_in_irq = prev_irq;
                deferred_call_execute_pending();
                auto prev_raised = m_in_critical.exchange(prev_critical);
                VERIFY(prev_raised == prev_critical + 1);
                check_invoke_scheduler();
            } else if (prev_critical == 0) {
                check_invoke_scheduler();
            }
        } else {
            m_in_irq = prev_irq;
        }
    }

    ALWAYS_INLINE u32& in_irq()
    {
        return m_in_irq;
    }

    ALWAYS_INLINE void restore_in_critical(u32 critical)
    {
        m_in_critical = critical;
    }

    ALWAYS_INLINE void enter_critical(u32& prev_flags)
    {
        prev_flags = cpu_flags();
        cli();
        m_in_critical++;
    }

    ALWAYS_INLINE void leave_critical(u32 prev_flags)
    {
        cli(); // Need to prevent IRQs from interrupting us here!
        VERIFY(m_in_critical > 0);
        if (m_in_critical == 1) {
            if (!m_in_irq) {
                deferred_call_execute_pending();
                VERIFY(m_in_critical == 1);
            }
            m_in_critical--;
            if (!m_in_irq)
                check_invoke_scheduler();
        } else {
            m_in_critical--;
        }
        if (prev_flags & 0x200)
            sti();
        else
            cli();
    }

    ALWAYS_INLINE u32 clear_critical(u32& prev_flags, bool enable_interrupts)
    {
        prev_flags = cpu_flags();
        u32 prev_crit = m_in_critical.exchange(0, AK::MemoryOrder::memory_order_acquire);
        if (!m_in_irq)
            check_invoke_scheduler();
        if (enable_interrupts)
            sti();
        return prev_crit;
    }

    ALWAYS_INLINE void restore_critical(u32 prev_crit, u32 prev_flags)
    {
        m_in_critical.store(prev_crit, AK::MemoryOrder::memory_order_release);
        VERIFY(!prev_crit || !(prev_flags & 0x200));
        if (prev_flags & 0x200)
            sti();
        else
            cli();
    }

    ALWAYS_INLINE u32 in_critical() { return m_in_critical.load(); }

    ALWAYS_INLINE const FPUState& clean_fpu_state() const
    {
        return s_clean_fpu_state;
    }

    static void smp_enable();
    bool smp_process_pending_messages();

    template<typename Callback>
    static void smp_broadcast(Callback callback, bool async)
    {
        auto* data = new Callback(move(callback));
        smp_broadcast(
            [](void* data) {
                (*reinterpret_cast<Callback*>(data))();
            },
            data,
            [](void* data) {
                delete reinterpret_cast<Callback*>(data);
            },
            async);
    }
    static void smp_broadcast(void (*callback)(), bool async);
    static void smp_broadcast(void (*callback)(void*), void* data, void (*free_data)(void*), bool async);
    template<typename Callback>
    static void smp_unicast(u32 cpu, Callback callback, bool async)
    {
        auto* data = new Callback(move(callback));
        smp_unicast(
            cpu,
            [](void* data) {
                (*reinterpret_cast<Callback*>(data))();
            },
            data,
            [](void* data) {
                delete reinterpret_cast<Callback*>(data);
            },
            async);
    }
    static void smp_unicast(u32 cpu, void (*callback)(), bool async);
    static void smp_unicast(u32 cpu, void (*callback)(void*), void* data, void (*free_data)(void*), bool async);
    static void smp_broadcast_flush_tlb(const PageDirectory*, VirtualAddress, size_t);
    static u32 smp_wake_n_idle_processors(u32 wake_count);

    template<typename Callback>
    static void deferred_call_queue(Callback callback)
    {
        auto* data = new Callback(move(callback));
        deferred_call_queue(
            [](void* data) {
                (*reinterpret_cast<Callback*>(data))();
            },
            data,
            [](void* data) {
                delete reinterpret_cast<Callback*>(data);
            });
    }
    static void deferred_call_queue(void (*callback)());
    static void deferred_call_queue(void (*callback)(void*), void* data, void (*free_data)(void*));

    ALWAYS_INLINE bool has_feature(CPUFeature f) const
    {
        return (static_cast<u32>(m_features) & static_cast<u32>(f)) != 0;
    }

    void check_invoke_scheduler();
    void invoke_scheduler_async() { m_invoke_scheduler_async = true; }

    void enter_trap(TrapFrame& trap, bool raise_irq);

    void exit_trap(TrapFrame& trap);

    [[noreturn]] void initialize_context_switching(Thread& initial_thread);
    void switch_context(Thread*& from_thread, Thread*& to_thread);
    [[noreturn]] static void assume_context(Thread& thread, u32 flags);
    u32 init_context(Thread& thread, bool leave_crit);
    static Vector<FlatPtr> capture_stack_trace(Thread& thread, size_t max_frames = 0);

    String platform_string() const;
};

class ScopedCritical {
    AK_MAKE_NONCOPYABLE(ScopedCritical);

public:
    ScopedCritical()
    {
        enter();
    }

    ~ScopedCritical()
    {
        if (m_valid)
            leave();
    }

    ScopedCritical(ScopedCritical&& from)
        : m_prev_flags(exchange(from.m_prev_flags, 0))
        , m_valid(exchange(from.m_valid, false))
    {
    }

    ScopedCritical& operator=(ScopedCritical&& from)
    {
        if (&from != this) {
            m_prev_flags = exchange(from.m_prev_flags, 0);
            m_valid = exchange(from.m_valid, false);
        }
        return *this;
    }

    void leave()
    {
        VERIFY(m_valid);
        m_valid = false;
        Processor::current().leave_critical(m_prev_flags);
    }

    void enter()
    {
        VERIFY(!m_valid);
        m_valid = true;
        Processor::current().enter_critical(m_prev_flags);
    }

private:
    u32 m_prev_flags { 0 };
    bool m_valid { false };
};

struct TrapFrame {
    u32 prev_irq_level;
    TrapFrame* next_trap;
    RegisterState* regs; // must be last

    TrapFrame() = delete;
    TrapFrame(const TrapFrame&) = delete;
    TrapFrame(TrapFrame&&) = delete;
    TrapFrame& operator=(const TrapFrame&) = delete;
    TrapFrame& operator=(TrapFrame&&) = delete;
};

#if ARCH(I386)
#    define TRAP_FRAME_SIZE (3 * 4)
#else
#    define TRAP_FRAME_SIZE (3 * 8)
#endif

static_assert(TRAP_FRAME_SIZE == sizeof(TrapFrame));

extern "C" void enter_trap_no_irq(TrapFrame*);
extern "C" void enter_trap(TrapFrame*);
extern "C" void exit_trap(TrapFrame*);

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

ALWAYS_INLINE void stac()
{
    if (!Processor::current().has_feature(CPUFeature::SMAP))
        return;
    asm volatile("stac" ::
                     : "cc");
}

ALWAYS_INLINE void clac()
{
    if (!Processor::current().has_feature(CPUFeature::SMAP))
        return;
    asm volatile("clac" ::
                     : "cc");
}

}
