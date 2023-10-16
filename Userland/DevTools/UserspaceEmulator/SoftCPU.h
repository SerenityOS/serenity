/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Emulator.h"
#include "Region.h"
#include "SoftFPU.h"
#include "SoftVPU.h"
#include "ValueWithShadow.h"
#include <AK/ByteReader.h>
#include <AK/Debug.h>
#include <LibDisassembly/Instruction.h>
#include <LibDisassembly/Interpreter.h>

namespace UserspaceEmulator {

class Emulator;
class Region;

union PartAddressableRegister {
    struct {
        u32 full_u32;
    };
    struct {
        u16 low_u16;
        u16 high_u16;
    };
    struct {
        u8 low_u8;
        u8 high_u8;
        u16 also_high_u16;
    };
};

class SoftCPU final
    : public Disassembly::X86::Interpreter
    , public Disassembly::X86::InstructionStream {
    friend SoftFPU;

public:
    using ValueWithShadowType8 = ValueWithShadow<u8>;
    using ValueWithShadowType16 = ValueWithShadow<u16>;
    using ValueWithShadowType32 = ValueWithShadow<u32>;
    using ValueWithShadowType64 = ValueWithShadow<u64>;
    using ValueWithShadowType128 = ValueWithShadow<u128>;
    using ValueWithShadowType256 = ValueWithShadow<u256>;

    explicit SoftCPU(Emulator&);
    void dump() const;

    u32 base_eip() const { return m_base_eip; }
    void save_base_eip() { m_base_eip = m_eip; }

    u32 eip() const { return m_eip; }
    void set_eip(u32 eip)
    {
        m_eip = eip;
    }

    struct Flags {
        enum Flag {
            CF = 0x0001, // 0b0000'0000'0000'0001
            PF = 0x0004, // 0b0000'0000'0000'0100
            AF = 0x0010, // 0b0000'0000'0001'0000
            ZF = 0x0040, // 0b0000'0000'0100'0000
            SF = 0x0080, // 0b0000'0000'1000'0000
            TF = 0x0100, // 0b0000'0001'0000'0000
            IF = 0x0200, // 0b0000'0010'0000'0000
            DF = 0x0400, // 0b0000'0100'0000'0000
            OF = 0x0800, // 0b0000'1000'0000'0000
        };
    };

    void push32(ValueWithShadow<u32>);
    ValueWithShadow<u32> pop32();

    void push16(ValueWithShadow<u16>);
    ValueWithShadow<u16> pop16();

    void push_string(StringView);
    void push_buffer(u8 const* data, size_t);

    u16 segment(Disassembly::X86::SegmentRegister seg) const { return m_segment[(int)seg]; }
    u16& segment(Disassembly::X86::SegmentRegister seg) { return m_segment[(int)seg]; }

    ValueAndShadowReference<u8> gpr8(Disassembly::X86::RegisterIndex8 reg)
    {
        switch (reg) {
        case Disassembly::X86::RegisterAL:
            return m_gpr[Disassembly::X86::RegisterEAX].reference_to<&PartAddressableRegister::low_u8>();
        case Disassembly::X86::RegisterAH:
            return m_gpr[Disassembly::X86::RegisterEAX].reference_to<&PartAddressableRegister::high_u8>();
        case Disassembly::X86::RegisterBL:
            return m_gpr[Disassembly::X86::RegisterEBX].reference_to<&PartAddressableRegister::low_u8>();
        case Disassembly::X86::RegisterBH:
            return m_gpr[Disassembly::X86::RegisterEBX].reference_to<&PartAddressableRegister::high_u8>();
        case Disassembly::X86::RegisterCL:
            return m_gpr[Disassembly::X86::RegisterECX].reference_to<&PartAddressableRegister::low_u8>();
        case Disassembly::X86::RegisterCH:
            return m_gpr[Disassembly::X86::RegisterECX].reference_to<&PartAddressableRegister::high_u8>();
        case Disassembly::X86::RegisterDL:
            return m_gpr[Disassembly::X86::RegisterEDX].reference_to<&PartAddressableRegister::low_u8>();
        case Disassembly::X86::RegisterDH:
            return m_gpr[Disassembly::X86::RegisterEDX].reference_to<&PartAddressableRegister::high_u8>();
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ValueWithShadow<u8> const_gpr8(Disassembly::X86::RegisterIndex8 reg) const
    {
        switch (reg) {
        case Disassembly::X86::RegisterAL:
            return m_gpr[Disassembly::X86::RegisterEAX].slice<&PartAddressableRegister::low_u8>();
        case Disassembly::X86::RegisterAH:
            return m_gpr[Disassembly::X86::RegisterEAX].slice<&PartAddressableRegister::high_u8>();
        case Disassembly::X86::RegisterBL:
            return m_gpr[Disassembly::X86::RegisterEBX].slice<&PartAddressableRegister::low_u8>();
        case Disassembly::X86::RegisterBH:
            return m_gpr[Disassembly::X86::RegisterEBX].slice<&PartAddressableRegister::high_u8>();
        case Disassembly::X86::RegisterCL:
            return m_gpr[Disassembly::X86::RegisterECX].slice<&PartAddressableRegister::low_u8>();
        case Disassembly::X86::RegisterCH:
            return m_gpr[Disassembly::X86::RegisterECX].slice<&PartAddressableRegister::high_u8>();
        case Disassembly::X86::RegisterDL:
            return m_gpr[Disassembly::X86::RegisterEDX].slice<&PartAddressableRegister::low_u8>();
        case Disassembly::X86::RegisterDH:
            return m_gpr[Disassembly::X86::RegisterEDX].slice<&PartAddressableRegister::high_u8>();
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ValueWithShadow<u16> const_gpr16(Disassembly::X86::RegisterIndex16 reg) const
    {
        return m_gpr[reg].slice<&PartAddressableRegister::low_u16>();
    }

    ValueAndShadowReference<u16> gpr16(Disassembly::X86::RegisterIndex16 reg)
    {
        return m_gpr[reg].reference_to<&PartAddressableRegister::low_u16>();
    }

    ValueWithShadow<u32> const_gpr32(Disassembly::X86::RegisterIndex32 reg) const
    {
        return m_gpr[reg].slice<&PartAddressableRegister::full_u32>();
    }

    ValueAndShadowReference<u32> gpr32(Disassembly::X86::RegisterIndex32 reg)
    {
        return m_gpr[reg].reference_to<&PartAddressableRegister::full_u32>();
    }

    template<typename T>
    ValueWithShadow<T> const_gpr(unsigned register_index) const
    {
        if constexpr (sizeof(T) == 1)
            return const_gpr8((Disassembly::X86::RegisterIndex8)register_index);
        if constexpr (sizeof(T) == 2)
            return const_gpr16((Disassembly::X86::RegisterIndex16)register_index);
        if constexpr (sizeof(T) == 4)
            return const_gpr32((Disassembly::X86::RegisterIndex32)register_index);
    }

    template<typename T>
    ValueAndShadowReference<T> gpr(unsigned register_index)
    {
        if constexpr (sizeof(T) == 1)
            return gpr8((Disassembly::X86::RegisterIndex8)register_index);
        if constexpr (sizeof(T) == 2)
            return gpr16((Disassembly::X86::RegisterIndex16)register_index);
        if constexpr (sizeof(T) == 4)
            return gpr32((Disassembly::X86::RegisterIndex32)register_index);
    }

    ValueWithShadow<u32> source_index(Disassembly::X86::AddressSize address_size) const
    {
        if (address_size == Disassembly::X86::AddressSize::Size32)
            return esi();
        if (address_size == Disassembly::X86::AddressSize::Size16)
            return { si().value(), (u32)si().shadow_as_value() & 0xffff };
        VERIFY_NOT_REACHED();
    }

    ValueWithShadow<u32> destination_index(Disassembly::X86::AddressSize address_size) const
    {
        if (address_size == Disassembly::X86::AddressSize::Size32)
            return edi();
        if (address_size == Disassembly::X86::AddressSize::Size16)
            return { di().value(), (u32)di().shadow_as_value() & 0xffff };
        VERIFY_NOT_REACHED();
    }

    ValueWithShadow<u32> loop_index(Disassembly::X86::AddressSize address_size) const
    {
        if (address_size == Disassembly::X86::AddressSize::Size32)
            return ecx();
        if (address_size == Disassembly::X86::AddressSize::Size16)
            return { cx().value(), (u32)cx().shadow_as_value() & 0xffff };
        VERIFY_NOT_REACHED();
    }

    bool decrement_loop_index(Disassembly::X86::AddressSize address_size)
    {
        switch (address_size) {
        case Disassembly::X86::AddressSize::Size32:
            set_ecx({ ecx().value() - 1, ecx().shadow() });
            return ecx().value() == 0;
        case Disassembly::X86::AddressSize::Size16:
            set_cx(ValueWithShadow<u16>(cx().value() - 1, cx().shadow()));
            return cx().value() == 0;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ALWAYS_INLINE void step_source_index(Disassembly::X86::AddressSize address_size, u32 step)
    {
        switch (address_size) {
        case Disassembly::X86::AddressSize::Size32:
            if (df())
                set_esi({ esi().value() - step, esi().shadow() });
            else
                set_esi({ esi().value() + step, esi().shadow() });
            break;
        case Disassembly::X86::AddressSize::Size16:
            if (df())
                set_si(ValueWithShadow<u16>(si().value() - step, si().shadow()));
            else
                set_si(ValueWithShadow<u16>(si().value() + step, si().shadow()));
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ALWAYS_INLINE void step_destination_index(Disassembly::X86::AddressSize address_size, u32 step)
    {
        switch (address_size) {
        case Disassembly::X86::AddressSize::Size32:
            if (df())
                set_edi({ edi().value() - step, edi().shadow() });
            else
                set_edi({ edi().value() + step, edi().shadow() });
            break;
        case Disassembly::X86::AddressSize::Size16:
            if (df())
                set_di(ValueWithShadow<u16>(di().value() - step, di().shadow()));
            else
                set_di(ValueWithShadow<u16>(di().value() + step, di().shadow()));
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    u32 eflags() const { return m_eflags; }
    void set_eflags(ValueWithShadow<u32> eflags)
    {
        m_eflags = eflags.value();
        m_flags_tainted = eflags.is_uninitialized();
    }

    ValueWithShadow<u32> eax() const { return const_gpr32(Disassembly::X86::RegisterEAX); }
    ValueWithShadow<u32> ebx() const { return const_gpr32(Disassembly::X86::RegisterEBX); }
    ValueWithShadow<u32> ecx() const { return const_gpr32(Disassembly::X86::RegisterECX); }
    ValueWithShadow<u32> edx() const { return const_gpr32(Disassembly::X86::RegisterEDX); }
    ValueWithShadow<u32> esp() const { return const_gpr32(Disassembly::X86::RegisterESP); }
    ValueWithShadow<u32> ebp() const { return const_gpr32(Disassembly::X86::RegisterEBP); }
    ValueWithShadow<u32> esi() const { return const_gpr32(Disassembly::X86::RegisterESI); }
    ValueWithShadow<u32> edi() const { return const_gpr32(Disassembly::X86::RegisterEDI); }

    ValueWithShadow<u16> ax() const { return const_gpr16(Disassembly::X86::RegisterAX); }
    ValueWithShadow<u16> bx() const { return const_gpr16(Disassembly::X86::RegisterBX); }
    ValueWithShadow<u16> cx() const { return const_gpr16(Disassembly::X86::RegisterCX); }
    ValueWithShadow<u16> dx() const { return const_gpr16(Disassembly::X86::RegisterDX); }
    ValueWithShadow<u16> sp() const { return const_gpr16(Disassembly::X86::RegisterSP); }
    ValueWithShadow<u16> bp() const { return const_gpr16(Disassembly::X86::RegisterBP); }
    ValueWithShadow<u16> si() const { return const_gpr16(Disassembly::X86::RegisterSI); }
    ValueWithShadow<u16> di() const { return const_gpr16(Disassembly::X86::RegisterDI); }

    ValueWithShadow<u8> al() const { return const_gpr8(Disassembly::X86::RegisterAL); }
    ValueWithShadow<u8> ah() const { return const_gpr8(Disassembly::X86::RegisterAH); }
    ValueWithShadow<u8> bl() const { return const_gpr8(Disassembly::X86::RegisterBL); }
    ValueWithShadow<u8> bh() const { return const_gpr8(Disassembly::X86::RegisterBH); }
    ValueWithShadow<u8> cl() const { return const_gpr8(Disassembly::X86::RegisterCL); }
    ValueWithShadow<u8> ch() const { return const_gpr8(Disassembly::X86::RegisterCH); }
    ValueWithShadow<u8> dl() const { return const_gpr8(Disassembly::X86::RegisterDL); }
    ValueWithShadow<u8> dh() const { return const_gpr8(Disassembly::X86::RegisterDH); }

    long double fpu_get(u8 index) { return m_fpu.fpu_get(index); }
    long double fpu_pop() { return m_fpu.fpu_pop(); }
    MMX mmx_get(u8 index) const { return m_fpu.mmx_get(index); }

    void set_eax(ValueWithShadow<u32> value) { gpr32(Disassembly::X86::RegisterEAX) = value; }
    void set_ebx(ValueWithShadow<u32> value) { gpr32(Disassembly::X86::RegisterEBX) = value; }
    void set_ecx(ValueWithShadow<u32> value) { gpr32(Disassembly::X86::RegisterECX) = value; }
    void set_edx(ValueWithShadow<u32> value) { gpr32(Disassembly::X86::RegisterEDX) = value; }
    void set_esp(ValueWithShadow<u32> value) { gpr32(Disassembly::X86::RegisterESP) = value; }
    void set_ebp(ValueWithShadow<u32> value) { gpr32(Disassembly::X86::RegisterEBP) = value; }
    void set_esi(ValueWithShadow<u32> value) { gpr32(Disassembly::X86::RegisterESI) = value; }
    void set_edi(ValueWithShadow<u32> value) { gpr32(Disassembly::X86::RegisterEDI) = value; }

    void set_ax(ValueWithShadow<u16> value) { gpr16(Disassembly::X86::RegisterAX) = value; }
    void set_bx(ValueWithShadow<u16> value) { gpr16(Disassembly::X86::RegisterBX) = value; }
    void set_cx(ValueWithShadow<u16> value) { gpr16(Disassembly::X86::RegisterCX) = value; }
    void set_dx(ValueWithShadow<u16> value) { gpr16(Disassembly::X86::RegisterDX) = value; }
    void set_sp(ValueWithShadow<u16> value) { gpr16(Disassembly::X86::RegisterSP) = value; }
    void set_bp(ValueWithShadow<u16> value) { gpr16(Disassembly::X86::RegisterBP) = value; }
    void set_si(ValueWithShadow<u16> value) { gpr16(Disassembly::X86::RegisterSI) = value; }
    void set_di(ValueWithShadow<u16> value) { gpr16(Disassembly::X86::RegisterDI) = value; }

    void set_al(ValueWithShadow<u8> value) { gpr8(Disassembly::X86::RegisterAL) = value; }
    void set_ah(ValueWithShadow<u8> value) { gpr8(Disassembly::X86::RegisterAH) = value; }
    void set_bl(ValueWithShadow<u8> value) { gpr8(Disassembly::X86::RegisterBL) = value; }
    void set_bh(ValueWithShadow<u8> value) { gpr8(Disassembly::X86::RegisterBH) = value; }
    void set_cl(ValueWithShadow<u8> value) { gpr8(Disassembly::X86::RegisterCL) = value; }
    void set_ch(ValueWithShadow<u8> value) { gpr8(Disassembly::X86::RegisterCH) = value; }
    void set_dl(ValueWithShadow<u8> value) { gpr8(Disassembly::X86::RegisterDL) = value; }
    void set_dh(ValueWithShadow<u8> value) { gpr8(Disassembly::X86::RegisterDH) = value; }

    void fpu_push(long double value) { m_fpu.fpu_push(value); }
    void fpu_set(u8 index, long double value) { m_fpu.fpu_set(index, value); }
    void mmx_set(u8 index, MMX value) { m_fpu.mmx_set(index, value); }

    bool of() const { return m_eflags & Flags::OF; }
    bool sf() const { return m_eflags & Flags::SF; }
    bool zf() const { return m_eflags & Flags::ZF; }
    bool af() const { return m_eflags & Flags::AF; }
    bool pf() const { return m_eflags & Flags::PF; }
    bool cf() const { return m_eflags & Flags::CF; }
    bool df() const { return m_eflags & Flags::DF; }

    void set_flag(Flags::Flag flag, bool value)
    {
        if (value)
            m_eflags |= flag;
        else
            m_eflags &= ~flag;
    }

    void set_of(bool value) { set_flag(Flags::OF, value); }
    void set_sf(bool value) { set_flag(Flags::SF, value); }
    void set_zf(bool value) { set_flag(Flags::ZF, value); }
    void set_af(bool value) { set_flag(Flags::AF, value); }
    void set_pf(bool value) { set_flag(Flags::PF, value); }
    void set_cf(bool value) { set_flag(Flags::CF, value); }
    void set_df(bool value) { set_flag(Flags::DF, value); }

    void set_flags_with_mask(u32 new_flags, u32 mask)
    {
        m_eflags &= ~mask;
        m_eflags |= new_flags & mask;
    }

    void set_flags_oszapc(u32 new_flags)
    {
        set_flags_with_mask(new_flags, Flags::OF | Flags::SF | Flags::ZF | Flags::AF | Flags::PF | Flags::CF);
    }

    void set_flags_oszap(u32 new_flags)
    {
        set_flags_with_mask(new_flags, Flags::OF | Flags::SF | Flags::ZF | Flags::AF | Flags::PF);
    }

    void set_flags_oszpc(u32 new_flags)
    {
        set_flags_with_mask(new_flags, Flags::OF | Flags::SF | Flags::ZF | Flags::PF | Flags::CF);
    }

    void set_flags_oc(u32 new_flags)
    {
        set_flags_with_mask(new_flags, Flags::OF | Flags::CF);
    }

    u16 cs() const { return m_segment[(int)Disassembly::X86::SegmentRegister::CS]; }
    u16 ds() const { return m_segment[(int)Disassembly::X86::SegmentRegister::DS]; }
    u16 es() const { return m_segment[(int)Disassembly::X86::SegmentRegister::ES]; }
    u16 ss() const { return m_segment[(int)Disassembly::X86::SegmentRegister::SS]; }
    u16 fs() const { return m_segment[(int)Disassembly::X86::SegmentRegister::FS]; }
    u16 gs() const { return m_segment[(int)Disassembly::X86::SegmentRegister::GS]; }

    ValueWithShadow<u8> read_memory8(Disassembly::X86::LogicalAddress);
    ValueWithShadow<u16> read_memory16(Disassembly::X86::LogicalAddress);
    ValueWithShadow<u32> read_memory32(Disassembly::X86::LogicalAddress);
    ValueWithShadow<u64> read_memory64(Disassembly::X86::LogicalAddress);
    ValueWithShadow<u128> read_memory128(Disassembly::X86::LogicalAddress);
    ValueWithShadow<u256> read_memory256(Disassembly::X86::LogicalAddress);

    template<typename T>
    ValueWithShadow<T> read_memory(Disassembly::X86::LogicalAddress address)
    {
        auto value = m_emulator.mmu().read<T>(address);
        if constexpr (AK::HasFormatter<T>)
            outln_if(MEMORY_DEBUG, "\033[36;1mread_memory: @{:#04x}:{:p} -> {:#064x} ({:hex-dump})\033[0m", address.selector(), address.offset(), value.value(), value.shadow().span());
        else
            outln_if(MEMORY_DEBUG, "\033[36;1mread_memory: @{:#04x}:{:p} -> ??? ({:hex-dump})\033[0m", address.selector(), address.offset(), value.shadow().span());
        return value;
    }

    void write_memory8(Disassembly::X86::LogicalAddress, ValueWithShadow<u8>);
    void write_memory16(Disassembly::X86::LogicalAddress, ValueWithShadow<u16>);
    void write_memory32(Disassembly::X86::LogicalAddress, ValueWithShadow<u32>);
    void write_memory64(Disassembly::X86::LogicalAddress, ValueWithShadow<u64>);
    void write_memory128(Disassembly::X86::LogicalAddress, ValueWithShadow<u128>);
    void write_memory256(Disassembly::X86::LogicalAddress, ValueWithShadow<u256>);

    template<typename T>
    void write_memory(Disassembly::X86::LogicalAddress address, ValueWithShadow<T> data)
    {
        if constexpr (sizeof(T) == 1)
            return write_memory8(address, data);
        if constexpr (sizeof(T) == 2)
            return write_memory16(address, data);
        if constexpr (sizeof(T) == 4)
            return write_memory32(address, data);
        if constexpr (sizeof(T) == 8)
            return write_memory64(address, data);
        if constexpr (sizeof(T) == 16)
            return write_memory128(address, data);
        if constexpr (sizeof(T) == 32)
            return write_memory256(address, data);
    }

    bool evaluate_condition(u8 condition) const
    {
        switch (condition) {
        case 0:
            return of(); // O
        case 1:
            return !of(); // NO
        case 2:
            return cf(); // B, C, NAE
        case 3:
            return !cf(); // NB, NC, AE
        case 4:
            return zf(); // E, Z
        case 5:
            return !zf(); // NE, NZ
        case 6:
            return cf() || zf(); // BE, NA
        case 7:
            return !(cf() || zf()); // NBE, A
        case 8:
            return sf(); // S
        case 9:
            return !sf(); // NS
        case 10:
            return pf(); // P, PE
        case 11:
            return !pf(); // NP, PO
        case 12:
            return sf() != of(); // L, NGE
        case 13:
            return sf() == of(); // NL, GE
        case 14:
            return (sf() != of()) || zf(); // LE, NG
        case 15:
            return !((sf() != of()) || zf()); // NLE, G
        default:
            VERIFY_NOT_REACHED();
        }
        return 0;
    }

    template<bool check_zf, typename Callback>
    void do_once_or_repeat(Disassembly::X86::Instruction const& insn, Callback);

    template<typename A>
    void taint_flags_from(A const& a)
    {
        m_flags_tainted = a.is_uninitialized();
    }

    template<typename A, typename B>
    void taint_flags_from(A const& a, B const& b)
    {
        m_flags_tainted = a.is_uninitialized() || b.is_uninitialized();
    }

    template<typename A, typename B, typename C>
    void taint_flags_from(A const& a, B const& b, C const& c)
    {
        m_flags_tainted = a.is_uninitialized() || b.is_uninitialized() || c.is_uninitialized();
    }

    void warn_if_flags_tainted(char const* message) const;

    // ^Disassembly::X86::InstructionStream
    virtual bool can_read() override { return false; }
    virtual u8 read8() override;
    virtual u16 read16() override;
    virtual u32 read32() override;
    virtual u64 read64() override;

private:
    // ^Disassembly::X86::Interpreter
    virtual void AAA(Disassembly::X86::Instruction const&) override;
    virtual void AAD(Disassembly::X86::Instruction const&) override;
    virtual void AAM(Disassembly::X86::Instruction const&) override;
    virtual void AAS(Disassembly::X86::Instruction const&) override;
    virtual void ADC_AL_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ADC_AX_imm16(Disassembly::X86::Instruction const&) override;
    virtual void ADC_EAX_imm32(Disassembly::X86::Instruction const&) override;
    virtual void ADC_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void ADC_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ADC_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void ADC_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void ADC_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ADC_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void ADC_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ADC_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void ADC_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void ADC_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void ADC_reg8_RM8(Disassembly::X86::Instruction const&) override;
    virtual void ADD_AL_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ADD_AX_imm16(Disassembly::X86::Instruction const&) override;
    virtual void ADD_EAX_imm32(Disassembly::X86::Instruction const&) override;
    virtual void ADD_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void ADD_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ADD_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void ADD_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void ADD_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ADD_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void ADD_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ADD_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void ADD_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void ADD_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void ADD_reg8_RM8(Disassembly::X86::Instruction const&) override;
    virtual void AND_AL_imm8(Disassembly::X86::Instruction const&) override;
    virtual void AND_AX_imm16(Disassembly::X86::Instruction const&) override;
    virtual void AND_EAX_imm32(Disassembly::X86::Instruction const&) override;
    virtual void AND_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void AND_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void AND_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void AND_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void AND_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void AND_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void AND_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void AND_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void AND_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void AND_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void AND_reg8_RM8(Disassembly::X86::Instruction const&) override;
    virtual void ARPL(Disassembly::X86::Instruction const&) override;
    virtual void BOUND(Disassembly::X86::Instruction const&) override;
    virtual void BSF_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void BSF_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void BSR_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void BSR_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void BSWAP_reg32(Disassembly::X86::Instruction const&) override;
    virtual void BTC_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void BTC_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void BTC_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void BTC_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void BTR_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void BTR_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void BTR_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void BTR_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void BTS_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void BTS_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void BTS_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void BTS_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void BT_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void BT_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void BT_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void BT_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void CALL_FAR_mem16(Disassembly::X86::Instruction const&) override;
    virtual void CALL_FAR_mem32(Disassembly::X86::Instruction const&) override;
    virtual void CALL_RM16(Disassembly::X86::Instruction const&) override;
    virtual void CALL_RM32(Disassembly::X86::Instruction const&) override;
    virtual void CALL_imm16(Disassembly::X86::Instruction const&) override;
    virtual void CALL_imm16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void CALL_imm16_imm32(Disassembly::X86::Instruction const&) override;
    virtual void CALL_imm32(Disassembly::X86::Instruction const&) override;
    virtual void CBW(Disassembly::X86::Instruction const&) override;
    virtual void CDQ(Disassembly::X86::Instruction const&) override;
    virtual void CLC(Disassembly::X86::Instruction const&) override;
    virtual void CLD(Disassembly::X86::Instruction const&) override;
    virtual void CLI(Disassembly::X86::Instruction const&) override;
    virtual void CLTS(Disassembly::X86::Instruction const&) override;
    virtual void CMC(Disassembly::X86::Instruction const&) override;
    virtual void CMOVcc_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void CMOVcc_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void CMPSB(Disassembly::X86::Instruction const&) override;
    virtual void CMPSD(Disassembly::X86::Instruction const&) override;
    virtual void CMPSW(Disassembly::X86::Instruction const&) override;
    virtual void CMPXCHG_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void CMPXCHG_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void CMPXCHG_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void CMP_AL_imm8(Disassembly::X86::Instruction const&) override;
    virtual void CMP_AX_imm16(Disassembly::X86::Instruction const&) override;
    virtual void CMP_EAX_imm32(Disassembly::X86::Instruction const&) override;
    virtual void CMP_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void CMP_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void CMP_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void CMP_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void CMP_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void CMP_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void CMP_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void CMP_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void CMP_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void CMP_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void CMP_reg8_RM8(Disassembly::X86::Instruction const&) override;
    virtual void CPUID(Disassembly::X86::Instruction const&) override;
    virtual void CWD(Disassembly::X86::Instruction const&) override;
    virtual void CWDE(Disassembly::X86::Instruction const&) override;
    virtual void DAA(Disassembly::X86::Instruction const&) override;
    virtual void DAS(Disassembly::X86::Instruction const&) override;
    virtual void DEC_RM16(Disassembly::X86::Instruction const&) override;
    virtual void DEC_RM32(Disassembly::X86::Instruction const&) override;
    virtual void DEC_RM8(Disassembly::X86::Instruction const&) override;
    virtual void DEC_reg16(Disassembly::X86::Instruction const&) override;
    virtual void DEC_reg32(Disassembly::X86::Instruction const&) override;
    virtual void DIV_RM16(Disassembly::X86::Instruction const&) override;
    virtual void DIV_RM32(Disassembly::X86::Instruction const&) override;
    virtual void DIV_RM8(Disassembly::X86::Instruction const&) override;
    virtual void ENTER16(Disassembly::X86::Instruction const&) override;
    virtual void ENTER32(Disassembly::X86::Instruction const&) override;
    virtual void ESCAPE(Disassembly::X86::Instruction const&) override;
    virtual void FADD_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FMUL_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FCOM_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FCOMP_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FSUB_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FSUBR_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FDIV_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FDIVR_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FLD_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FXCH(Disassembly::X86::Instruction const&) override;
    virtual void FST_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FNOP(Disassembly::X86::Instruction const&) override;
    virtual void FSTP_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FLDENV(Disassembly::X86::Instruction const&) override;
    virtual void FCHS(Disassembly::X86::Instruction const&) override;
    virtual void FABS(Disassembly::X86::Instruction const&) override;
    virtual void FTST(Disassembly::X86::Instruction const&) override;
    virtual void FXAM(Disassembly::X86::Instruction const&) override;
    virtual void FLDCW(Disassembly::X86::Instruction const&) override;
    virtual void FLD1(Disassembly::X86::Instruction const&) override;
    virtual void FLDL2T(Disassembly::X86::Instruction const&) override;
    virtual void FLDL2E(Disassembly::X86::Instruction const&) override;
    virtual void FLDPI(Disassembly::X86::Instruction const&) override;
    virtual void FLDLG2(Disassembly::X86::Instruction const&) override;
    virtual void FLDLN2(Disassembly::X86::Instruction const&) override;
    virtual void FLDZ(Disassembly::X86::Instruction const&) override;
    virtual void FNSTENV(Disassembly::X86::Instruction const&) override;
    virtual void F2XM1(Disassembly::X86::Instruction const&) override;
    virtual void FYL2X(Disassembly::X86::Instruction const&) override;
    virtual void FPTAN(Disassembly::X86::Instruction const&) override;
    virtual void FPATAN(Disassembly::X86::Instruction const&) override;
    virtual void FXTRACT(Disassembly::X86::Instruction const&) override;
    virtual void FPREM1(Disassembly::X86::Instruction const&) override;
    virtual void FDECSTP(Disassembly::X86::Instruction const&) override;
    virtual void FINCSTP(Disassembly::X86::Instruction const&) override;
    virtual void FNSTCW(Disassembly::X86::Instruction const&) override;
    virtual void FPREM(Disassembly::X86::Instruction const&) override;
    virtual void FYL2XP1(Disassembly::X86::Instruction const&) override;
    virtual void FSQRT(Disassembly::X86::Instruction const&) override;
    virtual void FSINCOS(Disassembly::X86::Instruction const&) override;
    virtual void FRNDINT(Disassembly::X86::Instruction const&) override;
    virtual void FSCALE(Disassembly::X86::Instruction const&) override;
    virtual void FSIN(Disassembly::X86::Instruction const&) override;
    virtual void FCOS(Disassembly::X86::Instruction const&) override;
    virtual void FIADD_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FCMOVB(Disassembly::X86::Instruction const&) override;
    virtual void FIMUL_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FCMOVE(Disassembly::X86::Instruction const&) override;
    virtual void FICOM_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FCMOVBE(Disassembly::X86::Instruction const&) override;
    virtual void FICOMP_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FCMOVU(Disassembly::X86::Instruction const&) override;
    virtual void FISUB_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FISUBR_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FUCOMPP(Disassembly::X86::Instruction const&) override;
    virtual void FIDIV_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FIDIVR_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FILD_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FCMOVNB(Disassembly::X86::Instruction const&) override;
    virtual void FISTTP_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FCMOVNE(Disassembly::X86::Instruction const&) override;
    virtual void FIST_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FCMOVNBE(Disassembly::X86::Instruction const&) override;
    virtual void FISTP_RM32(Disassembly::X86::Instruction const&) override;
    virtual void FCMOVNU(Disassembly::X86::Instruction const&) override;
    virtual void FNENI(Disassembly::X86::Instruction const&) override;
    virtual void FNDISI(Disassembly::X86::Instruction const&) override;
    virtual void FNCLEX(Disassembly::X86::Instruction const&) override;
    virtual void FNINIT(Disassembly::X86::Instruction const&) override;
    virtual void FNSETPM(Disassembly::X86::Instruction const&) override;
    virtual void FLD_RM80(Disassembly::X86::Instruction const&) override;
    virtual void FUCOMI(Disassembly::X86::Instruction const&) override;
    virtual void FCOMI(Disassembly::X86::Instruction const&) override;
    virtual void FSTP_RM80(Disassembly::X86::Instruction const&) override;
    virtual void FADD_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FMUL_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FCOM_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FCOMP_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FSUB_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FSUBR_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FDIV_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FDIVR_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FLD_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FFREE(Disassembly::X86::Instruction const&) override;
    virtual void FISTTP_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FST_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FSTP_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FRSTOR(Disassembly::X86::Instruction const&) override;
    virtual void FUCOM(Disassembly::X86::Instruction const&) override;
    virtual void FUCOMP(Disassembly::X86::Instruction const&) override;
    virtual void FNSAVE(Disassembly::X86::Instruction const&) override;
    virtual void FNSTSW(Disassembly::X86::Instruction const&) override;
    virtual void FIADD_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FADDP(Disassembly::X86::Instruction const&) override;
    virtual void FIMUL_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FMULP(Disassembly::X86::Instruction const&) override;
    virtual void FICOM_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FICOMP_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FCOMPP(Disassembly::X86::Instruction const&) override;
    virtual void FISUB_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FSUBRP(Disassembly::X86::Instruction const&) override;
    virtual void FISUBR_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FSUBP(Disassembly::X86::Instruction const&) override;
    virtual void FIDIV_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FDIVRP(Disassembly::X86::Instruction const&) override;
    virtual void FIDIVR_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FDIVP(Disassembly::X86::Instruction const&) override;
    virtual void FILD_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FFREEP(Disassembly::X86::Instruction const&) override;
    virtual void FISTTP_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FIST_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FISTP_RM16(Disassembly::X86::Instruction const&) override;
    virtual void FBLD_M80(Disassembly::X86::Instruction const&) override;
    virtual void FNSTSW_AX(Disassembly::X86::Instruction const&) override;
    virtual void FILD_RM64(Disassembly::X86::Instruction const&) override;
    virtual void FUCOMIP(Disassembly::X86::Instruction const&) override;
    virtual void FBSTP_M80(Disassembly::X86::Instruction const&) override;
    virtual void FCOMIP(Disassembly::X86::Instruction const&) override;
    virtual void FISTP_RM64(Disassembly::X86::Instruction const&) override;
    virtual void HLT(Disassembly::X86::Instruction const&) override;
    virtual void IDIV_RM16(Disassembly::X86::Instruction const&) override;
    virtual void IDIV_RM32(Disassembly::X86::Instruction const&) override;
    virtual void IDIV_RM8(Disassembly::X86::Instruction const&) override;
    virtual void IMUL_RM16(Disassembly::X86::Instruction const&) override;
    virtual void IMUL_RM32(Disassembly::X86::Instruction const&) override;
    virtual void IMUL_RM8(Disassembly::X86::Instruction const&) override;
    virtual void IMUL_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void IMUL_reg16_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void IMUL_reg16_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void IMUL_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void IMUL_reg32_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void IMUL_reg32_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void INC_RM16(Disassembly::X86::Instruction const&) override;
    virtual void INC_RM32(Disassembly::X86::Instruction const&) override;
    virtual void INC_RM8(Disassembly::X86::Instruction const&) override;
    virtual void INC_reg16(Disassembly::X86::Instruction const&) override;
    virtual void INC_reg32(Disassembly::X86::Instruction const&) override;
    virtual void INSB(Disassembly::X86::Instruction const&) override;
    virtual void INSD(Disassembly::X86::Instruction const&) override;
    virtual void INSW(Disassembly::X86::Instruction const&) override;
    virtual void INT1(Disassembly::X86::Instruction const&) override;
    virtual void INT3(Disassembly::X86::Instruction const&) override;
    virtual void INTO(Disassembly::X86::Instruction const&) override;
    virtual void INT_imm8(Disassembly::X86::Instruction const&) override;
    virtual void INVLPG(Disassembly::X86::Instruction const&) override;
    virtual void IN_AL_DX(Disassembly::X86::Instruction const&) override;
    virtual void IN_AL_imm8(Disassembly::X86::Instruction const&) override;
    virtual void IN_AX_DX(Disassembly::X86::Instruction const&) override;
    virtual void IN_AX_imm8(Disassembly::X86::Instruction const&) override;
    virtual void IN_EAX_DX(Disassembly::X86::Instruction const&) override;
    virtual void IN_EAX_imm8(Disassembly::X86::Instruction const&) override;
    virtual void IRET(Disassembly::X86::Instruction const&) override;
    virtual void JCXZ_imm8(Disassembly::X86::Instruction const&) override;
    virtual void JMP_FAR_mem16(Disassembly::X86::Instruction const&) override;
    virtual void JMP_FAR_mem32(Disassembly::X86::Instruction const&) override;
    virtual void JMP_RM16(Disassembly::X86::Instruction const&) override;
    virtual void JMP_RM32(Disassembly::X86::Instruction const&) override;
    virtual void JMP_imm16(Disassembly::X86::Instruction const&) override;
    virtual void JMP_imm16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void JMP_imm16_imm32(Disassembly::X86::Instruction const&) override;
    virtual void JMP_imm32(Disassembly::X86::Instruction const&) override;
    virtual void JMP_short_imm8(Disassembly::X86::Instruction const&) override;
    virtual void Jcc_NEAR_imm(Disassembly::X86::Instruction const&) override;
    virtual void Jcc_imm8(Disassembly::X86::Instruction const&) override;
    virtual void LAHF(Disassembly::X86::Instruction const&) override;
    virtual void LAR_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void LAR_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void LDS_reg16_mem16(Disassembly::X86::Instruction const&) override;
    virtual void LDS_reg32_mem32(Disassembly::X86::Instruction const&) override;
    virtual void LEAVE16(Disassembly::X86::Instruction const&) override;
    virtual void LEAVE32(Disassembly::X86::Instruction const&) override;
    virtual void LEA_reg16_mem16(Disassembly::X86::Instruction const&) override;
    virtual void LEA_reg32_mem32(Disassembly::X86::Instruction const&) override;
    virtual void LES_reg16_mem16(Disassembly::X86::Instruction const&) override;
    virtual void LES_reg32_mem32(Disassembly::X86::Instruction const&) override;
    virtual void LFS_reg16_mem16(Disassembly::X86::Instruction const&) override;
    virtual void LFS_reg32_mem32(Disassembly::X86::Instruction const&) override;
    virtual void LGDT(Disassembly::X86::Instruction const&) override;
    virtual void LGS_reg16_mem16(Disassembly::X86::Instruction const&) override;
    virtual void LGS_reg32_mem32(Disassembly::X86::Instruction const&) override;
    virtual void LIDT(Disassembly::X86::Instruction const&) override;
    virtual void LLDT_RM16(Disassembly::X86::Instruction const&) override;
    virtual void LMSW_RM16(Disassembly::X86::Instruction const&) override;
    virtual void LODSB(Disassembly::X86::Instruction const&) override;
    virtual void LODSD(Disassembly::X86::Instruction const&) override;
    virtual void LODSW(Disassembly::X86::Instruction const&) override;
    virtual void LOOPNZ_imm8(Disassembly::X86::Instruction const&) override;
    virtual void LOOPZ_imm8(Disassembly::X86::Instruction const&) override;
    virtual void LOOP_imm8(Disassembly::X86::Instruction const&) override;
    virtual void LSL_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void LSL_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void LSS_reg16_mem16(Disassembly::X86::Instruction const&) override;
    virtual void LSS_reg32_mem32(Disassembly::X86::Instruction const&) override;
    virtual void LTR_RM16(Disassembly::X86::Instruction const&) override;
    virtual void MOVSB(Disassembly::X86::Instruction const&) override;
    virtual void MOVSD(Disassembly::X86::Instruction const&) override;
    virtual void MOVSW(Disassembly::X86::Instruction const&) override;
    virtual void MOVSX_reg16_RM8(Disassembly::X86::Instruction const&) override;
    virtual void MOVSX_reg32_RM16(Disassembly::X86::Instruction const&) override;
    virtual void MOVSX_reg32_RM8(Disassembly::X86::Instruction const&) override;
    virtual void MOVZX_reg16_RM8(Disassembly::X86::Instruction const&) override;
    virtual void MOVZX_reg32_RM16(Disassembly::X86::Instruction const&) override;
    virtual void MOVZX_reg32_RM8(Disassembly::X86::Instruction const&) override;
    virtual void MOV_AL_moff8(Disassembly::X86::Instruction const&) override;
    virtual void MOV_AX_moff16(Disassembly::X86::Instruction const&) override;
    virtual void MOV_CR_reg32(Disassembly::X86::Instruction const&) override;
    virtual void MOV_DR_reg32(Disassembly::X86::Instruction const&) override;
    virtual void MOV_EAX_moff32(Disassembly::X86::Instruction const&) override;
    virtual void MOV_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void MOV_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void MOV_RM16_seg(Disassembly::X86::Instruction const&) override;
    virtual void MOV_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void MOV_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void MOV_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void MOV_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void MOV_moff16_AX(Disassembly::X86::Instruction const&) override;
    virtual void MOV_moff32_EAX(Disassembly::X86::Instruction const&) override;
    virtual void MOV_moff8_AL(Disassembly::X86::Instruction const&) override;
    virtual void MOV_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void MOV_reg16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void MOV_reg32_CR(Disassembly::X86::Instruction const&) override;
    virtual void MOV_reg32_DR(Disassembly::X86::Instruction const&) override;
    virtual void MOV_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void MOV_reg32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void MOV_reg8_RM8(Disassembly::X86::Instruction const&) override;
    virtual void MOV_reg8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void MOV_seg_RM16(Disassembly::X86::Instruction const&) override;
    virtual void MOV_seg_RM32(Disassembly::X86::Instruction const&) override;
    virtual void MUL_RM16(Disassembly::X86::Instruction const&) override;
    virtual void MUL_RM32(Disassembly::X86::Instruction const&) override;
    virtual void MUL_RM8(Disassembly::X86::Instruction const&) override;
    virtual void NEG_RM16(Disassembly::X86::Instruction const&) override;
    virtual void NEG_RM32(Disassembly::X86::Instruction const&) override;
    virtual void NEG_RM8(Disassembly::X86::Instruction const&) override;
    virtual void NOP(Disassembly::X86::Instruction const&) override;
    virtual void NOT_RM16(Disassembly::X86::Instruction const&) override;
    virtual void NOT_RM32(Disassembly::X86::Instruction const&) override;
    virtual void NOT_RM8(Disassembly::X86::Instruction const&) override;
    virtual void OR_AL_imm8(Disassembly::X86::Instruction const&) override;
    virtual void OR_AX_imm16(Disassembly::X86::Instruction const&) override;
    virtual void OR_EAX_imm32(Disassembly::X86::Instruction const&) override;
    virtual void OR_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void OR_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void OR_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void OR_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void OR_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void OR_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void OR_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void OR_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void OR_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void OR_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void OR_reg8_RM8(Disassembly::X86::Instruction const&) override;
    virtual void OUTSB(Disassembly::X86::Instruction const&) override;
    virtual void OUTSD(Disassembly::X86::Instruction const&) override;
    virtual void OUTSW(Disassembly::X86::Instruction const&) override;
    virtual void OUT_DX_AL(Disassembly::X86::Instruction const&) override;
    virtual void OUT_DX_AX(Disassembly::X86::Instruction const&) override;
    virtual void OUT_DX_EAX(Disassembly::X86::Instruction const&) override;
    virtual void OUT_imm8_AL(Disassembly::X86::Instruction const&) override;
    virtual void OUT_imm8_AX(Disassembly::X86::Instruction const&) override;
    virtual void OUT_imm8_EAX(Disassembly::X86::Instruction const&) override;
    virtual void PACKSSDW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PACKSSWB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PACKUSWB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PADDB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PADDW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PADDD_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PADDSB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PADDSW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PADDUSB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PADDUSW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PAND_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PANDN_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PCMPEQB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PCMPEQW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PCMPEQD_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PCMPGTB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PCMPGTW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PCMPGTD_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PMADDWD_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PMULHW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PMULLW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void POPA(Disassembly::X86::Instruction const&) override;
    virtual void POPAD(Disassembly::X86::Instruction const&) override;
    virtual void POPF(Disassembly::X86::Instruction const&) override;
    virtual void POPFD(Disassembly::X86::Instruction const&) override;
    virtual void POP_DS(Disassembly::X86::Instruction const&) override;
    virtual void POP_ES(Disassembly::X86::Instruction const&) override;
    virtual void POP_FS(Disassembly::X86::Instruction const&) override;
    virtual void POP_GS(Disassembly::X86::Instruction const&) override;
    virtual void POP_RM16(Disassembly::X86::Instruction const&) override;
    virtual void POP_RM32(Disassembly::X86::Instruction const&) override;
    virtual void POP_SS(Disassembly::X86::Instruction const&) override;
    virtual void POP_reg16(Disassembly::X86::Instruction const&) override;
    virtual void POP_reg32(Disassembly::X86::Instruction const&) override;
    virtual void POR_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSLLW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSLLW_mm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSLLD_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSLLD_mm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSLLQ_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSLLQ_mm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSRAW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSRAW_mm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSRAD_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSRAD_mm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSRLW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSRLW_mm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSRLD_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSRLD_mm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSRLQ_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSRLQ_mm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSUBB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSUBW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSUBD_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSUBSB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSUBSW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSUBUSB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSUBUSW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PUNPCKHBW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PUNPCKHWD_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PUNPCKHDQ_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PUNPCKLBW_mm1_mm2m32(Disassembly::X86::Instruction const&) override;
    virtual void PUNPCKLWD_mm1_mm2m32(Disassembly::X86::Instruction const&) override;
    virtual void PUNPCKLDQ_mm1_mm2m32(Disassembly::X86::Instruction const&) override;
    virtual void PUSHA(Disassembly::X86::Instruction const&) override;
    virtual void PUSHAD(Disassembly::X86::Instruction const&) override;
    virtual void PUSHF(Disassembly::X86::Instruction const&) override;
    virtual void PUSHFD(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_CS(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_DS(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_ES(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_FS(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_GS(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_RM16(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_RM32(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_SP_8086_80186(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_SS(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_imm16(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_imm32(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_reg16(Disassembly::X86::Instruction const&) override;
    virtual void PUSH_reg32(Disassembly::X86::Instruction const&) override;
    virtual void PXOR_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void RCL_RM16_1(Disassembly::X86::Instruction const&) override;
    virtual void RCL_RM16_CL(Disassembly::X86::Instruction const&) override;
    virtual void RCL_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void RCL_RM32_1(Disassembly::X86::Instruction const&) override;
    virtual void RCL_RM32_CL(Disassembly::X86::Instruction const&) override;
    virtual void RCL_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void RCL_RM8_1(Disassembly::X86::Instruction const&) override;
    virtual void RCL_RM8_CL(Disassembly::X86::Instruction const&) override;
    virtual void RCL_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void RCR_RM16_1(Disassembly::X86::Instruction const&) override;
    virtual void RCR_RM16_CL(Disassembly::X86::Instruction const&) override;
    virtual void RCR_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void RCR_RM32_1(Disassembly::X86::Instruction const&) override;
    virtual void RCR_RM32_CL(Disassembly::X86::Instruction const&) override;
    virtual void RCR_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void RCR_RM8_1(Disassembly::X86::Instruction const&) override;
    virtual void RCR_RM8_CL(Disassembly::X86::Instruction const&) override;
    virtual void RCR_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void RDTSC(Disassembly::X86::Instruction const&) override;
    virtual void RET(Disassembly::X86::Instruction const&) override;
    virtual void RETF(Disassembly::X86::Instruction const&) override;
    virtual void RETF_imm16(Disassembly::X86::Instruction const&) override;
    virtual void RET_imm16(Disassembly::X86::Instruction const&) override;
    virtual void ROL_RM16_1(Disassembly::X86::Instruction const&) override;
    virtual void ROL_RM16_CL(Disassembly::X86::Instruction const&) override;
    virtual void ROL_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ROL_RM32_1(Disassembly::X86::Instruction const&) override;
    virtual void ROL_RM32_CL(Disassembly::X86::Instruction const&) override;
    virtual void ROL_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ROL_RM8_1(Disassembly::X86::Instruction const&) override;
    virtual void ROL_RM8_CL(Disassembly::X86::Instruction const&) override;
    virtual void ROL_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ROR_RM16_1(Disassembly::X86::Instruction const&) override;
    virtual void ROR_RM16_CL(Disassembly::X86::Instruction const&) override;
    virtual void ROR_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ROR_RM32_1(Disassembly::X86::Instruction const&) override;
    virtual void ROR_RM32_CL(Disassembly::X86::Instruction const&) override;
    virtual void ROR_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void ROR_RM8_1(Disassembly::X86::Instruction const&) override;
    virtual void ROR_RM8_CL(Disassembly::X86::Instruction const&) override;
    virtual void ROR_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SAHF(Disassembly::X86::Instruction const&) override;
    virtual void SALC(Disassembly::X86::Instruction const&) override;
    virtual void SAR_RM16_1(Disassembly::X86::Instruction const&) override;
    virtual void SAR_RM16_CL(Disassembly::X86::Instruction const&) override;
    virtual void SAR_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SAR_RM32_1(Disassembly::X86::Instruction const&) override;
    virtual void SAR_RM32_CL(Disassembly::X86::Instruction const&) override;
    virtual void SAR_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SAR_RM8_1(Disassembly::X86::Instruction const&) override;
    virtual void SAR_RM8_CL(Disassembly::X86::Instruction const&) override;
    virtual void SAR_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SBB_AL_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SBB_AX_imm16(Disassembly::X86::Instruction const&) override;
    virtual void SBB_EAX_imm32(Disassembly::X86::Instruction const&) override;
    virtual void SBB_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void SBB_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SBB_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void SBB_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void SBB_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SBB_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void SBB_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SBB_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void SBB_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void SBB_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void SBB_reg8_RM8(Disassembly::X86::Instruction const&) override;
    virtual void SCASB(Disassembly::X86::Instruction const&) override;
    virtual void SCASD(Disassembly::X86::Instruction const&) override;
    virtual void SCASW(Disassembly::X86::Instruction const&) override;
    virtual void SETcc_RM8(Disassembly::X86::Instruction const&) override;
    virtual void SGDT(Disassembly::X86::Instruction const&) override;
    virtual void SHLD_RM16_reg16_CL(Disassembly::X86::Instruction const&) override;
    virtual void SHLD_RM16_reg16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHLD_RM32_reg32_CL(Disassembly::X86::Instruction const&) override;
    virtual void SHLD_RM32_reg32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHL_RM16_1(Disassembly::X86::Instruction const&) override;
    virtual void SHL_RM16_CL(Disassembly::X86::Instruction const&) override;
    virtual void SHL_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHL_RM32_1(Disassembly::X86::Instruction const&) override;
    virtual void SHL_RM32_CL(Disassembly::X86::Instruction const&) override;
    virtual void SHL_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHL_RM8_1(Disassembly::X86::Instruction const&) override;
    virtual void SHL_RM8_CL(Disassembly::X86::Instruction const&) override;
    virtual void SHL_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHRD_RM16_reg16_CL(Disassembly::X86::Instruction const&) override;
    virtual void SHRD_RM16_reg16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHRD_RM32_reg32_CL(Disassembly::X86::Instruction const&) override;
    virtual void SHRD_RM32_reg32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHR_RM16_1(Disassembly::X86::Instruction const&) override;
    virtual void SHR_RM16_CL(Disassembly::X86::Instruction const&) override;
    virtual void SHR_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHR_RM32_1(Disassembly::X86::Instruction const&) override;
    virtual void SHR_RM32_CL(Disassembly::X86::Instruction const&) override;
    virtual void SHR_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHR_RM8_1(Disassembly::X86::Instruction const&) override;
    virtual void SHR_RM8_CL(Disassembly::X86::Instruction const&) override;
    virtual void SHR_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SIDT(Disassembly::X86::Instruction const&) override;
    virtual void SLDT_RM16(Disassembly::X86::Instruction const&) override;
    virtual void SMSW_RM16(Disassembly::X86::Instruction const&) override;
    virtual void STC(Disassembly::X86::Instruction const&) override;
    virtual void STD(Disassembly::X86::Instruction const&) override;
    virtual void STI(Disassembly::X86::Instruction const&) override;
    virtual void STOSB(Disassembly::X86::Instruction const&) override;
    virtual void STOSD(Disassembly::X86::Instruction const&) override;
    virtual void STOSW(Disassembly::X86::Instruction const&) override;
    virtual void STR_RM16(Disassembly::X86::Instruction const&) override;
    virtual void SUB_AL_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SUB_AX_imm16(Disassembly::X86::Instruction const&) override;
    virtual void SUB_EAX_imm32(Disassembly::X86::Instruction const&) override;
    virtual void SUB_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void SUB_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SUB_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void SUB_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void SUB_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SUB_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void SUB_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SUB_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void SUB_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void SUB_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void SUB_reg8_RM8(Disassembly::X86::Instruction const&) override;
    virtual void TEST_AL_imm8(Disassembly::X86::Instruction const&) override;
    virtual void TEST_AX_imm16(Disassembly::X86::Instruction const&) override;
    virtual void TEST_EAX_imm32(Disassembly::X86::Instruction const&) override;
    virtual void TEST_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void TEST_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void TEST_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void TEST_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void TEST_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void TEST_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void UD0(Disassembly::X86::Instruction const&) override;
    virtual void UD1(Disassembly::X86::Instruction const&) override;
    virtual void UD2(Disassembly::X86::Instruction const&) override;
    virtual void VERR_RM16(Disassembly::X86::Instruction const&) override;
    virtual void VERW_RM16(Disassembly::X86::Instruction const&) override;
    virtual void WAIT(Disassembly::X86::Instruction const&) override;
    virtual void WBINVD(Disassembly::X86::Instruction const&) override;
    virtual void XADD_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void XADD_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void XADD_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void XCHG_AX_reg16(Disassembly::X86::Instruction const&) override;
    virtual void XCHG_EAX_reg32(Disassembly::X86::Instruction const&) override;
    virtual void XCHG_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void XCHG_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void XCHG_reg8_RM8(Disassembly::X86::Instruction const&) override;
    virtual void XLAT(Disassembly::X86::Instruction const&) override;
    virtual void XOR_AL_imm8(Disassembly::X86::Instruction const&) override;
    virtual void XOR_AX_imm16(Disassembly::X86::Instruction const&) override;
    virtual void XOR_EAX_imm32(Disassembly::X86::Instruction const&) override;
    virtual void XOR_RM16_imm16(Disassembly::X86::Instruction const&) override;
    virtual void XOR_RM16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void XOR_RM16_reg16(Disassembly::X86::Instruction const&) override;
    virtual void XOR_RM32_imm32(Disassembly::X86::Instruction const&) override;
    virtual void XOR_RM32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void XOR_RM32_reg32(Disassembly::X86::Instruction const&) override;
    virtual void XOR_RM8_imm8(Disassembly::X86::Instruction const&) override;
    virtual void XOR_RM8_reg8(Disassembly::X86::Instruction const&) override;
    virtual void XOR_reg16_RM16(Disassembly::X86::Instruction const&) override;
    virtual void XOR_reg32_RM32(Disassembly::X86::Instruction const&) override;
    virtual void XOR_reg8_RM8(Disassembly::X86::Instruction const&) override;
    virtual void MOVQ_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void MOVQ_mm1m64_mm2(Disassembly::X86::Instruction const&) override;
    virtual void MOVD_mm1_rm32(Disassembly::X86::Instruction const&) override;
    virtual void MOVQ_mm1_rm64(Disassembly::X86::Instruction const&) override; // long mode
    virtual void MOVD_rm32_mm2(Disassembly::X86::Instruction const&) override;
    virtual void MOVQ_rm64_mm2(Disassembly::X86::Instruction const&) override; // long mode
    virtual void EMMS(Disassembly::X86::Instruction const&) override;

    virtual void CMPXCHG8B_m64(Disassembly::X86::Instruction const&) override;
    virtual void RDRAND_reg(Disassembly::X86::Instruction const&) override;
    virtual void RDSEED_reg(Disassembly::X86::Instruction const&) override;

    virtual void PREFETCHTNTA(Disassembly::X86::Instruction const&) override;
    virtual void PREFETCHT0(Disassembly::X86::Instruction const&) override;
    virtual void PREFETCHT1(Disassembly::X86::Instruction const&) override;
    virtual void PREFETCHT2(Disassembly::X86::Instruction const&) override;
    virtual void LDMXCSR(Disassembly::X86::Instruction const&) override;
    virtual void STMXCSR(Disassembly::X86::Instruction const&) override;
    virtual void MOVUPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MOVSS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void MOVUPS_xmm1m128_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void MOVSS_xmm1m32_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void MOVLPS_xmm1_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void MOVLPS_m64_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void UNPCKLPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void UNPCKHPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MOVHPS_xmm1_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void MOVHPS_m64_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void MOVAPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MOVAPS_xmm1m128_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void CVTPI2PS_xmm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void CVTSI2SS_xmm1_rm32(Disassembly::X86::Instruction const&) override;
    virtual void MOVNTPS_xmm1m128_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void CVTTPS2PI_mm1_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void CVTTSS2SI_r32_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void CVTPS2PI_xmm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void CVTSS2SI_r32_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void UCOMISS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void COMISS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void MOVMSKPS_reg_xmm(Disassembly::X86::Instruction const&) override;
    virtual void SQRTPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void SQRTSS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void RSQRTPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void RSQRTSS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void RCPPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void RCPSS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void ANDPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void ANDNPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void ORPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void XORPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void ADDPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void ADDSS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void MULPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MULSS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void SUBPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void SUBSS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void MINPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MINSS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void DIVPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void DIVSS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void MAXPS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MAXSS_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void PSHUFW_mm1_mm2m64_imm8(Disassembly::X86::Instruction const&) override;
    virtual void CMPPS_xmm1_xmm2m128_imm8(Disassembly::X86::Instruction const&) override;
    virtual void CMPSS_xmm1_xmm2m32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PINSRW_mm1_r32m16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PINSRW_xmm1_r32m16_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PEXTRW_reg_mm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PEXTRW_reg_xmm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHUFPS_xmm1_xmm2m128_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PMOVMSKB_reg_mm1(Disassembly::X86::Instruction const&) override;
    virtual void PMOVMSKB_reg_xmm1(Disassembly::X86::Instruction const&) override;
    virtual void PMINUB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PMINUB_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void PMAXUB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PMAXUB_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void PAVGB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PAVGB_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void PAVGW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PAVGW_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void PMULHUW_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PMULHUW_xmm1_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void MOVNTQ_m64_mm1(Disassembly::X86::Instruction const&) override;
    virtual void PMINSB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PMINSB_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void PMAXSB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PMAXSB_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void PSADBB_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PSADBB_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MASKMOVQ_mm1_mm2m64(Disassembly::X86::Instruction const&) override;

    virtual void MOVUPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MOVSD_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void MOVUPD_xmm1m128_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void MOVSD_xmm1m32_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void MOVLPD_xmm1_m64(Disassembly::X86::Instruction const&) override;
    virtual void MOVLPD_m64_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void UNPCKLPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void UNPCKHPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MOVHPD_xmm1_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void MOVAPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MOVAPD_xmm1m128_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void CVTPI2PD_xmm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void CVTSI2SD_xmm1_rm32(Disassembly::X86::Instruction const&) override;
    virtual void CVTTPD2PI_mm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void CVTTSS2SI_r32_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void CVTPD2PI_xmm1_mm2m128(Disassembly::X86::Instruction const&) override;
    virtual void CVTSD2SI_xmm1_rm64(Disassembly::X86::Instruction const&) override;
    virtual void UCOMISD_xmm1_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void COMISD_xmm1_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void MOVMSKPD_reg_xmm(Disassembly::X86::Instruction const&) override;
    virtual void SQRTPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void SQRTSD_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void ANDPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void ANDNPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void ORPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void XORPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void ADDPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void ADDSD_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void MULPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MULSD_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void CVTPS2PD_xmm1_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void CVTPD2PS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void CVTSS2SD_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void CVTSD2SS_xmm1_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void CVTDQ2PS_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void CVTPS2DQ_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void CVTTPS2DQ_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void SUBPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void SUBSD_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void MINPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MINSD_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void DIVPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void DIVSD_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void MAXPD_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MAXSD_xmm1_xmm2m32(Disassembly::X86::Instruction const&) override;
    virtual void PUNPCKLQDQ_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void PUNPCKHQDQ_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MOVDQA_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MOVDQU_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void PSHUFD_xmm1_xmm2m128_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSHUFHW_xmm1_xmm2m128_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSHUFLW_xmm1_xmm2m128_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSRLQ_xmm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSRLDQ_xmm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSLLQ_xmm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PSLLDQ_xmm1_imm8(Disassembly::X86::Instruction const&) override;
    virtual void MOVD_rm32_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void MOVQ_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void MOVDQA_xmm1m128_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void MOVDQU_xmm1m128_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void CMPPD_xmm1_xmm2m128_imm8(Disassembly::X86::Instruction const&) override;
    virtual void CMPSD_xmm1_xmm2m32_imm8(Disassembly::X86::Instruction const&) override;
    virtual void SHUFPD_xmm1_xmm2m128_imm8(Disassembly::X86::Instruction const&) override;
    virtual void PADDQ_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void MOVQ_xmm1m128_xmm2(Disassembly::X86::Instruction const&) override;
    virtual void MOVQ2DQ_xmm_mm(Disassembly::X86::Instruction const&) override;
    virtual void MOVDQ2Q_mm_xmm(Disassembly::X86::Instruction const&) override;
    virtual void CVTTPD2DQ_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void CVTPD2DQ_xmm1_xmm2m128(Disassembly::X86::Instruction const&) override;
    virtual void CVTDQ2PD_xmm1_xmm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PMULUDQ_mm1_mm2m64(Disassembly::X86::Instruction const&) override;
    virtual void PMULUDQ_mm1_mm2m128(Disassembly::X86::Instruction const&) override;
    virtual void PSUBQ_mm1_mm2m64(Disassembly::X86::Instruction const&) override;

    virtual void wrap_0xC0(Disassembly::X86::Instruction const&) override;
    virtual void wrap_0xC1_16(Disassembly::X86::Instruction const&) override;
    virtual void wrap_0xC1_32(Disassembly::X86::Instruction const&) override;
    virtual void wrap_0xD0(Disassembly::X86::Instruction const&) override;
    virtual void wrap_0xD1_16(Disassembly::X86::Instruction const&) override;
    virtual void wrap_0xD1_32(Disassembly::X86::Instruction const&) override;
    virtual void wrap_0xD2(Disassembly::X86::Instruction const&) override;
    virtual void wrap_0xD3_16(Disassembly::X86::Instruction const&) override;
    virtual void wrap_0xD3_32(Disassembly::X86::Instruction const&) override;

    template<bool update_dest, bool is_or, typename Op>
    void generic_AL_imm8(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_or, typename Op>
    void generic_AX_imm16(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_or, typename Op>
    void generic_EAX_imm32(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_or, typename Op>
    void generic_RM16_imm16(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_or, typename Op>
    void generic_RM16_imm8(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, typename Op>
    void generic_RM16_unsigned_imm8(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_zero_idiom_if_both_operands_same, typename Op>
    void generic_RM16_reg16(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_or, typename Op>
    void generic_RM32_imm32(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_or, typename Op>
    void generic_RM32_imm8(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, typename Op>
    void generic_RM32_unsigned_imm8(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_zero_idiom_if_both_operands_same, typename Op>
    void generic_RM32_reg32(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_or, typename Op>
    void generic_RM8_imm8(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_zero_idiom_if_both_operands_same, typename Op>
    void generic_RM8_reg8(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_zero_idiom_if_both_operands_same, typename Op>
    void generic_reg16_RM16(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_zero_idiom_if_both_operands_same, typename Op>
    void generic_reg32_RM32(Op, Disassembly::X86::Instruction const&);
    template<bool update_dest, bool is_zero_idiom_if_both_operands_same, typename Op>
    void generic_reg8_RM8(Op, Disassembly::X86::Instruction const&);

    template<typename Op>
    void generic_RM8_1(Op, Disassembly::X86::Instruction const&);
    template<typename Op>
    void generic_RM8_CL(Op, Disassembly::X86::Instruction const&);
    template<typename Op>
    void generic_RM16_1(Op, Disassembly::X86::Instruction const&);
    template<typename Op>
    void generic_RM16_CL(Op, Disassembly::X86::Instruction const&);
    template<typename Op>
    void generic_RM32_1(Op, Disassembly::X86::Instruction const&);
    template<typename Op>
    void generic_RM32_CL(Op, Disassembly::X86::Instruction const&);

    void update_code_cache();

    void write_segment_register(Disassembly::X86::SegmentRegister, ValueWithShadow<u16>);

    Emulator& m_emulator;
    SoftFPU m_fpu;
    SoftVPU m_vpu;

    ValueWithShadow<PartAddressableRegister> m_gpr[8];

    u16 m_segment[8] { 0 };
    u32 m_eflags { 0 };

    bool m_flags_tainted { false };

    u32 m_eip { 0 };
    u32 m_base_eip { 0 };

    Region* m_cached_code_region { nullptr };
    u8* m_cached_code_base_ptr { nullptr };
};

ALWAYS_INLINE u8 SoftCPU::read8()
{
    if (!m_cached_code_region || !m_cached_code_region->contains(m_eip))
        update_code_cache();

    u8 value = m_cached_code_base_ptr[m_eip - m_cached_code_region->base()];
    m_eip += 1;
    return value;
}

ALWAYS_INLINE u16 SoftCPU::read16()
{
    if (!m_cached_code_region || !m_cached_code_region->contains(m_eip))
        update_code_cache();

    u16 value;
    ByteReader::load<u16>(&m_cached_code_base_ptr[m_eip - m_cached_code_region->base()], value);
    m_eip += 2;
    return value;
}

ALWAYS_INLINE u32 SoftCPU::read32()
{
    if (!m_cached_code_region || !m_cached_code_region->contains(m_eip))
        update_code_cache();

    u32 value;
    ByteReader::load<u32>(&m_cached_code_base_ptr[m_eip - m_cached_code_region->base()], value);

    m_eip += 4;
    return value;
}

ALWAYS_INLINE u64 SoftCPU::read64()
{
    if (!m_cached_code_region || !m_cached_code_region->contains(m_eip))
        update_code_cache();

    u64 value;
    ByteReader::load<u64>(&m_cached_code_base_ptr[m_eip - m_cached_code_region->base()], value);

    m_eip += 8;
    return value;
}

}
