#pragma once

#include "types.h"

#define PAGE_SIZE 4096u

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

void gdt_init();
void idt_init();
void registerInterruptHandler(BYTE number, void (*f)());
void registerUserCallableInterruptHandler(BYTE number, void (*f)());
void flushIDT();
void flushGDT();
void loadTaskRegister(WORD selector);
WORD allocateGDTEntry();
Descriptor& getGDTEntry(WORD selector);
void writeGDTEntry(WORD selector, Descriptor&);

#define HANG asm volatile( "cli; hlt" );
#define LSW(x) ((DWORD)(x) & 0xFFFF)
#define MSW(x) (((DWORD)(x) >> 16) & 0xFFFF)
#define LSB(x) ((x) & 0xFF)
#define MSB(x) (((x)>>8) & 0xFF)

#define cli() asm volatile("cli")
#define sti() asm volatile("sti")

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
    PageFault(word code, LinearAddress address)
        : m_code(code)
        , m_address(address)
    {
    }

    LinearAddress address() const { return m_address; }
    word code() const { return m_code; }

    bool isNotPresent() const { return (m_code & 1) == PageFaultFlags::NotPresent; }
    bool isProtectionViolation() const { return (m_code & 1) == PageFaultFlags::ProtectionViolation; }
    bool isRead() const { return (m_code & 2) == PageFaultFlags::Read; }
    bool isWrite() const { return (m_code & 2) == PageFaultFlags::Write; }
    bool isUser() const { return (m_code & 4) == PageFaultFlags::UserMode; }
    bool isSupervisor() const { return (m_code & 4) == PageFaultFlags::SupervisorMode; }
    bool isInstructionFetch() const { return (m_code & 8) == PageFaultFlags::InstructionFetch; }

private:
    word m_code;
    LinearAddress m_address;
};

struct RegisterDump {
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

inline constexpr dword pageBaseOf(dword address)
{
    return address & 0xfffff000;
}

