/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SoftFPU.h"
#include "Emulator.h"
#include "SoftCPU.h"
#include "ValueWithShadow.h"

#include <AK/BitCast.h>
#include <AK/NumericLimits.h>
#include <AK/UFixedBigInt.h>

#include <unistd.h>

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

#define TODO_INSN()                                                                   \
    do {                                                                              \
        reportln("\n=={}== Unimplemented instruction: {}\n", getpid(), __FUNCTION__); \
        m_emulator.dump_backtrace();                                                  \
        _exit(0);                                                                     \
    } while (0)

template<typename T>
ALWAYS_INLINE void warn_if_uninitialized(T value_with_shadow, const char* message)
{
    if (value_with_shadow.is_uninitialized()) [[unlikely]] {
        reportln("\033[31;1mWarning! Use of uninitialized value: {}\033[0m\n", message);
        UserspaceEmulator::Emulator::the().dump_backtrace();
    }
}

namespace UserspaceEmulator {

ALWAYS_INLINE void SoftFPU::warn_if_mmx_absolute(u8 index) const
{
    if (m_reg_is_mmx[index]) [[unlikely]] {
        reportln("\033[31;1mWarning! Use of an MMX register as an FPU value ({} abs)\033[0m\n", index);
        m_emulator.dump_backtrace();
    }
}
ALWAYS_INLINE void SoftFPU::warn_if_fpu_absolute(u8 index) const
{
    if (!m_reg_is_mmx[index]) [[unlikely]] {
        reportln("\033[31;1mWarning! Use of an FPU value ({} abs)  as an MMX register\033[0m\n", index);
        m_emulator.dump_backtrace();
    }
}

ALWAYS_INLINE long double SoftFPU::fpu_get(u8 index)
{
    VERIFY(index < 8);
    if (!fpu_is_set(index))
        fpu_set_stack_underflow();
    warn_if_mmx_absolute(index);

    u8 effective_index = (m_fpu_stack_top + index) % 8;

    return m_storage[effective_index].fp;
}
ALWAYS_INLINE void SoftFPU::fpu_set_absolute(u8 index, long double value)
{
    VERIFY(index < 8);
    set_tag_from_value_absolute(index, value);
    m_storage[index].fp = value;
    m_reg_is_mmx[index] = false;
}
ALWAYS_INLINE void SoftFPU::fpu_set(u8 index, long double value)
{
    VERIFY(index < 8);
    fpu_set_absolute((m_fpu_stack_top + index) % 8, value);
}
ALWAYS_INLINE MMX SoftFPU::mmx_get(u8 index) const
{
    VERIFY(index < 8);
    warn_if_fpu_absolute(index);
    return m_storage[index].mmx;
}
ALWAYS_INLINE void SoftFPU::mmx_set(u8 index, MMX value)
{
    m_storage[index].mmx = value;
    // The high bytes are set to 0b11... to make the floating-point value NaN.
    // This way we are technically able to find out if we are reading the wrong
    // type, but this is still difficult, so we use our own lookup for that
    m_storage[index].__high = 0xFFFFU;
    m_reg_is_mmx[index] = true;
}

ALWAYS_INLINE void SoftFPU::fpu_push(long double value)
{
    if (fpu_is_set(7))
        fpu_set_stack_overflow();
    m_fpu_stack_top = (m_fpu_stack_top - 1u) % 8;

    fpu_set(0, value);
}

ALWAYS_INLINE long double SoftFPU::fpu_pop()
{
    warn_if_mmx_absolute(m_fpu_stack_top);

    if (!fpu_is_set(0))
        fpu_set_stack_underflow();

    auto ret = fpu_get(0);
    fpu_set_tag(0, FPU_Tag::Empty);
    m_fpu_stack_top = (m_fpu_stack_top + 1u) % 8;
    return ret;
}

ALWAYS_INLINE void SoftFPU::fpu_set_exception(FPU_Exception ex)
{
    switch (ex) {
    case FPU_Exception::StackFault:
        m_fpu_error_stackfault = 1;
        m_fpu_error_invalid = 1; // Implies InvalidOperation
        break;
    case FPU_Exception::InvalidOperation:
        m_fpu_error_invalid = 1;
        if (!m_fpu_mask_invalid)
            break;
        return;
    case FPU_Exception::DenormalizedOperand:
        m_fpu_error_denorm = 1;
        if (!m_fpu_mask_denorm)
            break;
        return;
    case FPU_Exception::ZeroDivide:
        m_fpu_error_zero_div = 1;
        if (!m_fpu_mask_zero_div)
            break;
        return;
    case FPU_Exception::Overflow:
        m_fpu_error_overflow = 1;
        if (!m_fpu_mask_overflow)
            break;
        return;
    case FPU_Exception::Underflow:
        m_fpu_error_underflow = 1;
        if (!m_fpu_mask_underflow)
            break;
        return;
    case FPU_Exception::Precision:
        m_fpu_error_precision = 1;
        if (!m_fpu_mask_precision)
            break;
        return;
    }

    // set exception bit
    m_fpu_error_summary = 1;

    // FIXME: set traceback
    // For that we need to get the currently executing instruction and
    // the previous eip

    // FIXME: Call FPU Exception handler
    reportln("Trying to call Exception handler from {}", fpu_exception_string(ex));
    fpu_dump_env();
    m_emulator.dump_backtrace();
    TODO();
}

template<Arithmetic T>
ALWAYS_INLINE T SoftFPU::fpu_round(long double value) const
{
    // FIXME: may need to set indefinite values manually
    switch (fpu_get_round_mode()) {
    case RoundingMode::NEAREST:
        return static_cast<T>(roundl(value));
    case RoundingMode::DOWN:
        return static_cast<T>(floorl(value));
    case RoundingMode::UP:
        return static_cast<T>(ceill(value));
    case RoundingMode::TRUNC:
        return static_cast<T>(truncl(value));
    default:
        VERIFY_NOT_REACHED();
    }
}

template<Arithmetic T>
ALWAYS_INLINE T SoftFPU::fpu_round_checked(long double value)
{
    T result = fpu_round<T>(value);
    if (result != value)
        fpu_set_exception(FPU_Exception::Precision);
    if (result > value)
        set_c1(1);
    else
        set_c1(0);
    return result;
}

template<FloatingPoint T>
ALWAYS_INLINE T SoftFPU::fpu_convert(long double value) const
{
    // FIXME: actually round the right way
    return static_cast<T>(value);
}
template<FloatingPoint T>
ALWAYS_INLINE T SoftFPU::fpu_convert_checked(long double value)
{
    T result = fpu_convert<T>(value);
    if (auto rnd = value - result) {
        if (rnd > 0)
            set_c1(1);
        else
            set_c1(0);
        fpu_set_exception(FPU_Exception::Precision);
    }
    return result;
}

// Instructions

// DATA TRANSFER
void SoftFPU::FLD_RM32(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_push(fpu_get(insn.modrm().register_index()));
    } else {
        auto new_f32 = insn.modrm().read32(m_cpu, insn);
        // FIXME: Respect shadow values
        fpu_push(bit_cast<float>(new_f32.value()));
    }
}
void SoftFPU::FLD_RM64(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto new_f64 = insn.modrm().read64(m_cpu, insn);
    // FIXME: Respect shadow values
    fpu_push(bit_cast<double>(new_f64.value()));
}
void SoftFPU::FLD_RM80(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());

    // long doubles can be up to 128 bits wide in memory for reasons (alignment) and only uses 80 bits of precision
    // GCC uses 12 bytes in 32 bit and 16 bytes in 64 bit mode
    // so in the 32 bit case we read a bit to much, but that shouldn't be an issue.
    // FIXME: Respect shadow values
    u128 new_f80 = insn.modrm().read128(m_cpu, insn).value();

    fpu_push(*(long double*)new_f80.bytes().data());
}

void SoftFPU::FST_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    float f32 = fpu_convert_checked<float>(fpu_get(0));

    if (fpu_is_set(0))
        insn.modrm().write32(m_cpu, insn, shadow_wrap_as_initialized(bit_cast<u32>(f32)));
    else
        insn.modrm().write32(m_cpu, insn, ValueWithShadow<u32>(bit_cast<u32>(f32), 0u));
}
void SoftFPU::FST_RM64(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(0));
    } else {
        double f64 = fpu_convert_checked<double>(fpu_get(0));
        if (fpu_is_set(0))
            insn.modrm().write64(m_cpu, insn, shadow_wrap_as_initialized(bit_cast<u64>(f64)));
        else
            insn.modrm().write64(m_cpu, insn, ValueWithShadow<u64>(bit_cast<u64>(f64), 0ULL));
    }
}

void SoftFPU::FSTP_RM32(const X86::Instruction& insn)
{
    FST_RM32(insn);
    fpu_pop();
}
void SoftFPU::FSTP_RM64(const X86::Instruction& insn)
{
    FST_RM64(insn);
    fpu_pop();
}
void SoftFPU::FSTP_RM80(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(0));
        fpu_pop();
    } else {
        // FIXME: Respect more shadow values
        // long doubles can be up to 128 bits wide in memory for reasons (alignment) and only uses 80 bits of precision
        // gcc uses 12 byte in 32 bit and 16 byte in 64 bit mode
        // due to only 10 bytes being used, we just write these 10 into memory
        // We have to do .bytes().data() to get around static type analysis
        ValueWithShadow<u128> f80 { 0u, 0u };
        u128 value {};
        f80 = insn.modrm().read128(m_cpu, insn);
        *(long double*)value.bytes().data() = fpu_pop();
        memcpy(f80.value().bytes().data(), &value, 10); // copy
        memset(f80.shadow().bytes().data(), 0x01, 10);  // mark as initialized
        insn.modrm().write128(m_cpu, insn, f80);
    }
}

void SoftFPU::FILD_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = insn.modrm().read16(m_cpu, insn);
    warn_if_uninitialized(m16int, "int16 loaded as float");

    fpu_push(static_cast<long double>(static_cast<i16>(m16int.value())));
}
void SoftFPU::FILD_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = insn.modrm().read32(m_cpu, insn);
    warn_if_uninitialized(m32int, "int32 loaded as float");

    fpu_push(static_cast<long double>(static_cast<i32>(m32int.value())));
}
void SoftFPU::FILD_RM64(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m64int = insn.modrm().read64(m_cpu, insn);
    warn_if_uninitialized(m64int, "int64 loaded as float");

    fpu_push(static_cast<long double>(static_cast<i64>(m64int.value())));
}

void SoftFPU::FIST_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto f = fpu_get(0);
    set_c1(0);
    auto int16 = fpu_round_checked<i16>(f);

    // FIXME: Respect shadow values
    insn.modrm().write16(m_cpu, insn, shadow_wrap_as_initialized(bit_cast<u16>(int16)));
}
void SoftFPU::FIST_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto f = fpu_get(0);
    set_c1(0);
    auto int32 = fpu_round_checked<i32>(f);
    // FIXME: Respect shadow values
    insn.modrm().write32(m_cpu, insn, shadow_wrap_as_initialized(bit_cast<u32>(int32)));
}

void SoftFPU::FISTP_RM16(const X86::Instruction& insn)
{
    FIST_RM16(insn);
    fpu_pop();
}
void SoftFPU::FISTP_RM32(const X86::Instruction& insn)
{
    FIST_RM32(insn);
    fpu_pop();
}
void SoftFPU::FISTP_RM64(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto f = fpu_pop();
    set_c1(0);
    auto i64 = fpu_round_checked<int64_t>(f);
    // FIXME: Respect shadow values
    insn.modrm().write64(m_cpu, insn, shadow_wrap_as_initialized(bit_cast<u64>(i64)));
}

void SoftFPU::FISTTP_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    set_c1(0);
    i16 value = static_cast<i16>(fpu_pop());
    // FIXME: Respect shadow values
    insn.modrm().write16(m_cpu, insn, shadow_wrap_as_initialized(bit_cast<u16>(value)));
}
void SoftFPU::FISTTP_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    i32 value = static_cast<i32>(fpu_pop());
    set_c1(0);
    // FIXME: Respect shadow values
    insn.modrm().write32(m_cpu, insn, shadow_wrap_as_initialized(bit_cast<u32>(value)));
}
void SoftFPU::FISTTP_RM64(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    set_c1(0);
    i64 value = static_cast<i64>(fpu_pop());
    // FIXME: Respect shadow values
    insn.modrm().write64(m_cpu, insn, shadow_wrap_as_initialized(bit_cast<u64>(value)));
}

void SoftFPU::FBLD_M80(const X86::Instruction&) { TODO_INSN(); }
void SoftFPU::FBSTP_M80(const X86::Instruction&) { TODO_INSN(); }

void SoftFPU::FXCH(const X86::Instruction& insn)
{
    // FIXME: implicit argument `D9 C9` -> st[0] <-> st[1]?
    VERIFY(insn.modrm().is_register());
    set_c1(0);
    auto tmp = fpu_get(0);
    fpu_set(0, fpu_get(insn.modrm().register_index()));
    fpu_set(insn.modrm().register_index(), tmp);
}

void SoftFPU::FCMOVE(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    if (m_cpu.zf())
        fpu_set(0, fpu_get(insn.modrm().rm()));
}
void SoftFPU::FCMOVNE(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    if (!m_cpu.zf())
        fpu_set(0, fpu_get((insn.modrm().reg_fpu())));
}

void SoftFPU::FCMOVB(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    if (m_cpu.cf())
        fpu_set(0, fpu_get(insn.modrm().rm()));
}
void SoftFPU::FCMOVNB(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    if (!m_cpu.cf())
        fpu_set(0, fpu_get(insn.modrm().rm()));
}
void SoftFPU::FCMOVBE(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    if (m_cpu.cf() || m_cpu.zf())
        fpu_set(0, fpu_get(insn.modrm().rm()));
}
void SoftFPU::FCMOVNBE(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    if (!(m_cpu.cf() || m_cpu.zf()))
        fpu_set(0, fpu_get(insn.modrm().rm()));
}

void SoftFPU::FCMOVU(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    if (m_cpu.pf())
        fpu_set(0, fpu_get((insn.modrm().reg_fpu())));
}
void SoftFPU::FCMOVNU(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    if (!m_cpu.pf())
        fpu_set(0, fpu_get((insn.modrm().reg_fpu())));
}

// BASIC ARITHMETIC
void SoftFPU::FADD_RM32(const X86::Instruction& insn)
{
    // XXX look at ::INC_foo for how mem/reg stuff is handled, and use that here too to make sure this is only called for mem32 ops
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(insn.modrm().register_index()) + fpu_get(0));
    } else {
        auto new_f32 = insn.modrm().read32(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        fpu_set(0, fpu_get(0) + f32);
    }
}
void SoftFPU::FADD_RM64(const X86::Instruction& insn)
{
    // XXX look at ::INC_foo for how mem/reg stuff is handled, and use that here too to make sure this is only called for mem64 ops
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) + fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        fpu_set(0, fpu_get(0) + f64);
    }
}
void SoftFPU::FADDP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) + fpu_get(0));
    fpu_pop();
}

void SoftFPU::FIADD_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(m_cpu, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) + (long double)m32int);
}
void SoftFPU::FIADD_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(m_cpu, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) + (long double)m16int);
}

void SoftFPU::FSUB_RM32(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(0) - fpu_get(insn.modrm().register_index()));
    } else {
        auto new_f32 = insn.modrm().read32(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        fpu_set(0, fpu_get(0) - f32);
    }
}
void SoftFPU::FSUB_RM64(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) - fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        fpu_set(0, fpu_get(0) - f64);
    }
}
void SoftFPU::FSUBP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) - fpu_get(0));
    fpu_pop();
}

void SoftFPU::FSUBR_RM32(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(insn.modrm().register_index()) - fpu_get(0));
    } else {
        auto new_f32 = insn.modrm().read32(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        fpu_set(0, f32 - fpu_get(0));
    }
}
void SoftFPU::FSUBR_RM64(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) - fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        fpu_set(0, f64 - fpu_get(0));
    }
}
void SoftFPU::FSUBRP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    fpu_set(insn.modrm().register_index(), fpu_get(0) - fpu_get(insn.modrm().register_index()));
    fpu_pop();
}

void SoftFPU::FISUB_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(m_cpu, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) - (long double)m32int);
}
void SoftFPU::FISUB_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(m_cpu, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) - (long double)m16int);
}

void SoftFPU::FISUBR_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(m_cpu, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, (long double)m16int - fpu_get(0));
}
void SoftFPU::FISUBR_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(m_cpu, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, (long double)m32int - fpu_get(0));
}

void SoftFPU::FMUL_RM32(const X86::Instruction& insn)
{
    // XXX look at ::INC_foo for how mem/reg stuff is handled, and use that here too to make sure this is only called for mem32 ops
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(0) * fpu_get(insn.modrm().register_index()));
    } else {
        auto new_f32 = insn.modrm().read32(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        fpu_set(0, fpu_get(0) * f32);
    }
}
void SoftFPU::FMUL_RM64(const X86::Instruction& insn)
{
    // XXX look at ::INC_foo for how mem/reg stuff is handled, and use that here too to make sure this is only called for mem64 ops
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) * fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        fpu_set(0, fpu_get(0) * f64);
    }
}
void SoftFPU::FMULP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) * fpu_get(0));
    fpu_pop();
}

void SoftFPU::FIMUL_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(m_cpu, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) * m32int);
}
void SoftFPU::FIMUL_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(m_cpu, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) * m16int);
}

void SoftFPU::FDIV_RM32(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(0) / fpu_get(insn.modrm().register_index()));
    } else {
        auto new_f32 = insn.modrm().read32(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
        fpu_set(0, fpu_get(0) / f32);
    }
}
void SoftFPU::FDIV_RM64(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) / fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
        fpu_set(0, fpu_get(0) / f64);
    }
}
void SoftFPU::FDIVP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
    fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) / fpu_get(0));
    fpu_pop();
}

void SoftFPU::FDIVR_RM32(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(insn.modrm().register_index()) / fpu_get(0));
    } else {
        auto new_f32 = insn.modrm().read32(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
        fpu_set(0, f32 / fpu_get(0));
    }
}
void SoftFPU::FDIVR_RM64(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        // XXX this is FDIVR, Instruction decodes this weirdly
        //fpu_set(insn.modrm().register_index(), fpu_get(0) / fpu_get(insn.modrm().register_index()));
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) / fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(m_cpu, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
        fpu_set(0, f64 / fpu_get(0));
    }
}
void SoftFPU::FDIVRP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
    fpu_set(insn.modrm().register_index(), fpu_get(0) / fpu_get(insn.modrm().register_index()));
    fpu_pop();
}

void SoftFPU::FIDIV_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(m_cpu, insn).value();
    // FIXME: Respect shadow values
    // FIXME: Raise IA on 0 / _=0, raise Z on finite / +-0
    fpu_set(0, fpu_get(0) / m16int);
}
void SoftFPU::FIDIV_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(m_cpu, insn).value();
    // FIXME: Respect shadow values
    // FIXME: Raise IA on 0 / _=0, raise Z on finite / +-0
    fpu_set(0, fpu_get(0) / m32int);
}

void SoftFPU::FIDIVR_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(m_cpu, insn).value();
    // FIXME: Respect shadow values
    // FIXME: Raise IA on 0 / _=0, raise Z on finite / +-0
    fpu_set(0, m16int / fpu_get(0));
}
void SoftFPU::FIDIVR_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(m_cpu, insn).value();
    // FIXME: Respect shadow values
    // FIXME: Raise IA on 0 / _=0, raise Z on finite / +-0
    fpu_set(0, m32int / fpu_get(0));
}

void SoftFPU::FPREM(const X86::Instruction&)
{
    // FIXME: There are some exponent shenanigans supposed to be here
    long double top = fpu_get(0);
    long double one = fpu_get(1);

    int Q = static_cast<int>(truncl(top / one));
    top = top - (one * Q);
    set_c2(0);
    set_c1(Q & 1);
    set_c3((Q >> 1) & 1);
    set_c0((Q >> 2) & 1);

    fpu_set(0, top);
}
void SoftFPU::FPREM1(const X86::Instruction&)
{
    // FIXME: There are some exponent shenanigans supposed to be here
    long double top = fpu_get(0);
    long double one = fpu_get(1);

    int Q = static_cast<int>(roundl(top / one));
    top = top - (one * Q);
    set_c2(0);
    set_c1(Q & 1);
    set_c3((Q >> 1) & 1);
    set_c0((Q >> 2) & 1);

    fpu_set(0, top);
}
void SoftFPU::FABS(const X86::Instruction&)
{
    set_c1(0);
    fpu_set(0, __builtin_fabsl(fpu_get(0)));
}
void SoftFPU::FCHS(const X86::Instruction&)
{
    set_c1(0);
    fpu_set(0, -fpu_get(0));
}

void SoftFPU::FRNDINT(const X86::Instruction&)
{
    auto res = fpu_round_checked<long double>(fpu_get(0));
    fpu_set(0, res);
}

void SoftFPU::FSCALE(const X86::Instruction&)
{
    // FIXME: set C1 upon stack overflow or if result was rounded
    fpu_set(0, fpu_get(0) * powl(2, truncl(fpu_get(1))));
}

void SoftFPU::FSQRT(const X86::Instruction&)
{
    // FIXME: set C1 upon stack overflow or if result was rounded
    fpu_set(0, sqrtl(fpu_get(0)));
}

void SoftFPU::FXTRACT(const X86::Instruction&) { TODO_INSN(); }

// COMPARISON

// FIXME: there may be an implicit argument, how is this conveyed by the insn
void SoftFPU::FCOM_RM32(const X86::Instruction&) { TODO_INSN(); }
void SoftFPU::FCOM_RM64(const X86::Instruction&) { TODO_INSN(); }
void SoftFPU::FCOMP_RM32(const X86::Instruction&) { TODO_INSN(); }
void SoftFPU::FCOMP_RM64(const X86::Instruction&) { TODO_INSN(); }
void SoftFPU::FCOMPP(const X86::Instruction&)
{
    if (fpu_isnan(0) || fpu_isnan(1)) {
        fpu_set_exception(FPU_Exception::InvalidOperation);
        if (m_fpu_mask_invalid)
            fpu_set_unordered();
    } else {
        set_c2(0);
        set_c0(fpu_get(0) < fpu_get(1));
        set_c3(fpu_get(0) == fpu_get(1));
    }
    fpu_pop();
    fpu_pop();
}

void SoftFPU::FUCOM(const X86::Instruction&) { TODO_INSN(); } // Needs QNaN detection
void SoftFPU::FUCOMP(const X86::Instruction&) { TODO_INSN(); }
void SoftFPU::FUCOMPP(const X86::Instruction&) { TODO_INSN(); }

void SoftFPU::FICOM_RM16(const X86::Instruction& insn)
{
    // FIXME: Check for denormals
    VERIFY(insn.modrm().is_register());
    auto val_shd = insn.modrm().read16(m_cpu, insn);
    warn_if_uninitialized(val_shd, "int16 compare to float");
    auto val = static_cast<i16>(val_shd.value());
    if (fpu_isnan(0)) {
        fpu_set_unordered();
    } else {
        set_c0(fpu_get(0) < val);
        set_c2(0);
        set_c3(fpu_get(0) == val);
    }
    set_c1(0);
}
void SoftFPU::FICOM_RM32(const X86::Instruction& insn)
{
    // FIXME: Check for denormals
    VERIFY(insn.modrm().is_register());
    auto val_shd = insn.modrm().read32(m_cpu, insn);
    warn_if_uninitialized(val_shd, "int32 compare to float");
    auto val = static_cast<i32>(val_shd.value());
    if (fpu_isnan(0)) {
        fpu_set_unordered();
    } else {
        set_c0(fpu_get(0) < val);
        set_c2(0);
        set_c3(fpu_get(0) == val);
    }
    set_c1(0);
}
void SoftFPU::FICOMP_RM16(const X86::Instruction& insn)
{
    FICOM_RM16(insn);
    fpu_pop();
}
void SoftFPU::FICOMP_RM32(const X86::Instruction& insn)
{
    FICOM_RM32(insn);
    fpu_pop();
}

void SoftFPU::FCOMI(const X86::Instruction& insn)
{
    auto i = insn.modrm().rm();
    // FIXME: QNaN / exception handling.
    set_c0(0);
    if (isnan(fpu_get(0)) || isnan(fpu_get(1))) {
        fpu_set_exception(FPU_Exception::InvalidOperation);
        m_cpu.set_zf(1);
        m_cpu.set_pf(1);
        m_cpu.set_cf(1);
    } else {
        m_cpu.set_zf(fpu_get(0) == fpu_get(i));
        m_cpu.set_pf(false);
        m_cpu.set_cf(fpu_get(0) < fpu_get(i));
    }
    if (!fpu_is_set(1))
        fpu_set_exception(FPU_Exception::Underflow);

    m_cpu.set_of(false);
    m_cpu.set_af(false);
    m_cpu.set_sf(false);

    // FIXME: Taint should be based on ST(0) and ST(i)
    m_cpu.m_flags_tainted = false;
}
void SoftFPU::FCOMIP(const X86::Instruction& insn)
{
    FCOMI(insn);
    fpu_pop();
}

void SoftFPU::FUCOMI(const X86::Instruction& insn)
{
    auto i = insn.modrm().rm();
    // FIXME: Unordered comparison checks.
    // FIXME: QNaN / exception handling.
    set_c1(0);
    if (fpu_isnan(0) || fpu_isnan(i)) {
        m_cpu.set_zf(true);
        m_cpu.set_pf(true);
        m_cpu.set_cf(true);
    } else {
        m_cpu.set_zf(fpu_get(0) == fpu_get(i));
        m_cpu.set_pf(false);
        m_cpu.set_cf(fpu_get(0) < fpu_get(i));
    }
    m_cpu.set_of(false);
    m_cpu.set_af(false);
    m_cpu.set_sf(false);

    // FIXME: Taint should be based on ST(0) and ST(i)
    m_cpu.m_flags_tainted = false;
}
void SoftFPU::FUCOMIP(const X86::Instruction& insn)
{
    FUCOMI(insn);
    fpu_pop();
}

void SoftFPU::FTST(const X86::Instruction&)
{
    // FIXME: maybe check for denormal
    set_c1(0);
    if (fpu_isnan(0))
        // raise #IA?
        fpu_set_unordered();
    else {
        set_c0(fpu_get(0) < 0.);
        set_c2(0);
        set_c3(fpu_get(0) == 0.);
    }
}
void SoftFPU::FXAM(const X86::Instruction&)
{
    if (m_reg_is_mmx[m_fpu_stack_top]) {
        // technically a subset of NaN/INF, with the Tag set to valid,
        // but we have our own helper for this
        set_c0(0);
        set_c2(0);
        set_c3(0);
    } else {
        switch (fpu_get_tag(0)) {
        case FPU_Tag::Valid:
            set_c0(0);
            set_c2(1);
            set_c3(0);
            break;
        case FPU_Tag::Zero:
            set_c0(1);
            set_c2(0);
            set_c3(0);
            break;
        case FPU_Tag::Special:
            if (isinf(fpu_get(0))) {
                set_c0(1);
                set_c2(1);
                set_c3(0);
            } else if (isnan(fpu_get(0))) {
                set_c0(1);
                set_c2(0);
                set_c3(0);
            } else {
                // denormalized
                set_c0(0);
                set_c2(1);
                set_c3(1);
            }
            break;
        case FPU_Tag::Empty:
            set_c0(1);
            set_c2(0);
            set_c3(1);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }
    set_c1(signbit(fpu_get(0)));
}

// TRANSCENDENTAL
void SoftFPU::FSIN(const X86::Instruction&)
{
    // FIXME: set C1 upon stack overflow or if result was rounded
    // FIXME: Set C2 to 1 if ST(0) is outside range of -2^63 to +2^63; else set to 0
    fpu_set(0, sinl(fpu_get(0)));
}
void SoftFPU::FCOS(const X86::Instruction&)
{
    // FIXME: set C1 upon stack overflow or if result was rounded
    // FIXME: Set C2 to 1 if ST(0) is outside range of -2^63 to +2^63; else set to 0
    fpu_set(0, cosl(fpu_get(0)));
}
void SoftFPU::FSINCOS(const X86::Instruction&)
{
    // FIXME: set C1 upon stack overflow or if result was rounded
    // FIXME: Set C2 to 1 if ST(0) is outside range of -2^63 to +2^63; else set to 0s
    long double sin = sinl(fpu_get(0));
    long double cos = cosl(fpu_get(0));
    fpu_set(0, sin);
    fpu_push(cos);
}

void SoftFPU::FPTAN(const X86::Instruction&)
{
    // FIXME: set C1 upon stack overflow or if result was rounded
    // FIXME: Set C2 to 1 if ST(0) is outside range of -2^63 to +2^63; else set to 0
    fpu_set(0, tanl(fpu_get(0)));
    fpu_push(1.0f);
}
void SoftFPU::FPATAN(const X86::Instruction&)
{
    // FIXME: set C1  on stack underflow, or on rounding
    // FIXME: Exceptions
    fpu_set(1, atan2l(fpu_get(1), fpu_get(0)));
    fpu_pop();
}

void SoftFPU::F2XM1(const X86::Instruction&)
{
    // FIXME: validate ST(0) is in range –1.0 to +1.0
    auto val = fpu_get(0);
    // FIXME: Set C0, C2, C3 in FPU status word.
    fpu_set(0, powl(2, val) - 1.0l);
}
void SoftFPU::FYL2X(const X86::Instruction&)
{
    // FIXME: raise precision and under/overflow
    // FIXME: detect denormal operands
    // FIXME: QNaN
    auto f0 = fpu_get(0);
    auto f1 = fpu_get(1);

    if (f0 < 0. || isnan(f0) || isnan(f1)
        || (isinf(f0) && f1 == 0.) || (f0 == 1. && isinf(f1)))
        fpu_set_exception(FPU_Exception::InvalidOperation);
    if (f0 == 0.)
        fpu_set_exception(FPU_Exception::ZeroDivide);

    fpu_set(1, f1 * log2l(f0));
    fpu_pop();
}
void SoftFPU::FYL2XP1(const X86::Instruction&)
{
    // FIXME: raise #O #U #P #D
    // FIXME: QNaN
    auto f0 = fpu_get(0);
    auto f1 = fpu_get(1);
    if (isnan(f0) || isnan(f1)
        || (isinf(f1) && f0 == 0))
        fpu_set_exception(FPU_Exception::InvalidOperation);

    fpu_set(1, (f1 * log2l(f0 + 1.0l)));
    fpu_pop();
}

// LOAD CONSTANT
void SoftFPU::FLD1(const X86::Instruction&)
{
    fpu_push(1.0l);
}
void SoftFPU::FLDZ(const X86::Instruction&)
{
    fpu_push(0.0l);
}
void SoftFPU::FLDPI(const X86::Instruction&)
{
    fpu_push(M_PIl);
}
void SoftFPU::FLDL2E(const X86::Instruction&)
{
    fpu_push(M_LOG2El);
}
void SoftFPU::FLDLN2(const X86::Instruction&)
{
    fpu_push(M_LN2l);
}
void SoftFPU::FLDL2T(const X86::Instruction&)
{
    fpu_push(log2l(10.0l));
}
void SoftFPU::FLDLG2(const X86::Instruction&)
{
    fpu_push(log10l(2.0l));
}

// CONTROL
void SoftFPU::FINCSTP(const X86::Instruction&)
{
    m_fpu_stack_top = (m_fpu_stack_top + 1u) % 8u;
    set_c1(0);
}
void SoftFPU::FDECSTP(const X86::Instruction&)
{
    m_fpu_stack_top = (m_fpu_stack_top - 1u) % 8u;
    set_c1(0);
}

void SoftFPU::FFREE(const X86::Instruction& insn)
{
    fpu_set_tag(insn.modrm().reg_fpu(), FPU_Tag::Empty);
}
void SoftFPU::FFREEP(const X86::Instruction& insn)
{
    FFREE(insn);
    fpu_pop();
}

void SoftFPU::FNINIT(const X86::Instruction&)
{
    m_fpu_cw = 0x037F;
    m_fpu_sw = 0;
    m_fpu_tw = 0xFFFF;

    m_fpu_ip = 0;
    m_fpu_cs = 0;

    m_fpu_dp = 0;
    m_fpu_ds = 0;

    m_fpu_iop = 0;
};
void SoftFPU::FNCLEX(const X86::Instruction&)
{
    m_fpu_error_invalid = 0;
    m_fpu_error_denorm = 0;
    m_fpu_error_zero_div = 0;
    m_fpu_error_overflow = 0;
    m_fpu_error_underflow = 0;
    m_fpu_error_precision = 0;
    m_fpu_error_stackfault = 0;
    m_fpu_busy = 0;
}

void SoftFPU::FNSTCW(const X86::Instruction& insn)
{
    insn.modrm().write16(m_cpu, insn, shadow_wrap_as_initialized(m_fpu_cw));
}
void SoftFPU::FLDCW(const X86::Instruction& insn)
{
    m_fpu_cw = insn.modrm().read16(m_cpu, insn).value();
}

void SoftFPU::FNSTENV(const X86::Instruction& insn)
{
    // Assuming we are always in Protected mode
    // FIXME: 16-bit Format

    // 32-bit Format
    /* 31--------------16---------------0
     * |                |       CW      | 0
     * +----------------+---------------+
     * |                |       SW      | 4
     * +----------------+---------------+
     * |                |       TW      | 8
     * +----------------+---------------+
     * |               FIP              | 12
     * +----+-----------+---------------+ 
     * |0000|fpuOp[10:0]|    FIP_sel    | 16
     * +----+-----------+---------------+ 
     * |               FDP              | 20
     * +----------------+---------------+ 
     * |                |    FDP_ds     | 24
     * +----------------|---------------+ 
     * */

    auto address = insn.modrm().resolve(m_cpu, insn);

    m_cpu.write_memory16(address, shadow_wrap_as_initialized(m_fpu_cw));
    address.set_offset(address.offset() + 4);
    m_cpu.write_memory16(address, shadow_wrap_as_initialized(m_fpu_sw));
    address.set_offset(address.offset() + 4);
    m_cpu.write_memory16(address, shadow_wrap_as_initialized(m_fpu_tw));
    address.set_offset(address.offset() + 4);
    m_cpu.write_memory32(address, shadow_wrap_as_initialized(m_fpu_ip));
    address.set_offset(address.offset() + 4);
    m_cpu.write_memory16(address, shadow_wrap_as_initialized(m_fpu_cs));
    address.set_offset(address.offset() + 2);
    m_cpu.write_memory16(address, shadow_wrap_as_initialized<u16>(m_fpu_iop & 0x3FFU));
    address.set_offset(address.offset() + 2);
    m_cpu.write_memory32(address, shadow_wrap_as_initialized(m_fpu_dp));
    address.set_offset(address.offset() + 4);
    m_cpu.write_memory16(address, shadow_wrap_as_initialized(m_fpu_ds));
}
void SoftFPU::FLDENV(const X86::Instruction& insn)
{
    // Assuming we are always in Protected mode
    // FIXME: 16-bit Format
    auto address = insn.modrm().resolve(m_cpu, insn);

    // FIXME: Shadow Values
    m_fpu_cw = m_cpu.read_memory16(address).value();
    address.set_offset(address.offset() + 4);
    m_fpu_sw = m_cpu.read_memory16(address).value();
    address.set_offset(address.offset() + 4);
    m_fpu_tw = m_cpu.read_memory16(address).value();
    address.set_offset(address.offset() + 4);
    m_fpu_ip = m_cpu.read_memory32(address).value();
    address.set_offset(address.offset() + 4);
    m_fpu_cs = m_cpu.read_memory16(address).value();
    address.set_offset(address.offset() + 2);
    m_fpu_iop = m_cpu.read_memory16(address).value();
    address.set_offset(address.offset() + 2);
    m_fpu_dp = m_cpu.read_memory32(address).value();
    address.set_offset(address.offset() + 4);
    m_fpu_ds = m_cpu.read_memory16(address).value();
}

void SoftFPU::FNSAVE(const X86::Instruction& insn)
{
    FNSTENV(insn);

    auto address = insn.modrm().resolve(m_cpu, insn);
    address.set_offset(address.offset() + 28); // size of the ENV

    // write fpu-stack to memory
    u8 raw_data[80];
    for (int i = 0; i < 8; ++i) {
        memcpy(raw_data + 10 * i, &m_storage[i], 10);
    }
    for (int i = 0; i < 5; ++i) {
        // FIXME: Shadow Value
        m_cpu.write_memory128(address, shadow_wrap_as_initialized(((u128*)raw_data)[i]));
        address.set_offset(address.offset() + 16);
    }

    FNINIT(insn);
}
void SoftFPU::FRSTOR(const X86::Instruction& insn)
{
    FLDENV(insn);

    auto address = insn.modrm().resolve(m_cpu, insn);
    address.set_offset(address.offset() + 28); // size of the ENV

    // read fpu-stack from memory
    u8 raw_data[80];
    for (int i = 0; i < 5; ++i) {
        // FIXME: Shadow Value
        ((u128*)raw_data)[i] = m_cpu.read_memory128(address).value();
        address.set_offset(address.offset() + 16);
    }
    for (int i = 0; i < 8; ++i) {
        memcpy(&m_storage[i], raw_data + 10 * i, 10);
    }

    memset(m_reg_is_mmx, 0, sizeof(m_reg_is_mmx));
}

void SoftFPU::FNSTSW(const X86::Instruction& insn)
{
    insn.modrm().write16(m_cpu, insn, shadow_wrap_as_initialized(m_fpu_sw));
}
void SoftFPU::FNSTSW_AX(const X86::Instruction&)
{
    m_cpu.set_ax(shadow_wrap_as_initialized(m_fpu_sw));
}
// FIXME: FWAIT
void SoftFPU::FNOP(const X86::Instruction&) { }

// DO NOTHING?
void SoftFPU::FNENI(const X86::Instruction&) { TODO_INSN(); }
void SoftFPU::FNDISI(const X86::Instruction&) { TODO_INSN(); }
void SoftFPU::FNSETPM(const X86::Instruction&) { TODO_INSN(); }

// MMX
// helpers
#define LOAD_MM_MM64M()                                                                \
    MMX mm;                                                                            \
    MMX mm64m;                                                                         \
    if (insn.modrm().mod() == 0b11) { /* 0b11 signals a register */                    \
        mm64m = mmx_get(insn.modrm().rm());                                            \
    } else {                                                                           \
        auto temp = insn.modrm().read64(m_cpu, insn);                                  \
        warn_if_uninitialized(temp, "Read of uninitialized Memory as Packed integer"); \
        mm64m.raw = temp.value();                                                      \
    }                                                                                  \
    mm = mmx_get(insn.modrm().reg())

#define MMX_intrinsic(intrinsic, res_type, actor_type)                         \
    LOAD_MM_MM64M();                                                           \
    mm.res_type = __builtin_ia32_##intrinsic(mm.actor_type, mm64m.actor_type); \
    mmx_set(insn.modrm().reg(), mm);                                           \
    mmx_common();

// ARITHMETIC
void SoftFPU::PADDB_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();
    mm.v8 += mm64m.v8;
    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PADDW_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();
    mm.v16 += mm64m.v16;
    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PADDD_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();
    mm.v32 += mm64m.v32;
    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PADDSB_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(paddsb, v8, v8);
}
void SoftFPU::PADDSW_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(paddsw, v16, v16);
}
void SoftFPU::PADDUSB_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(paddusb, v8, v8);
}
void SoftFPU::PADDUSW_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(paddusw, v16, v16);
}

void SoftFPU::PSUBB_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();
    mm.v8 -= mm64m.v8;
    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSUBW_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();
    mm.v16 -= mm64m.v16;
    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSUBD_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();
    mm.v32 -= mm64m.v32;
    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSUBSB_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(psubsb, v8, v8);
}
void SoftFPU::PSUBSW_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(psubsw, v16, v16);
}
void SoftFPU::PSUBUSB_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(psubusb, v8, v8);
}
void SoftFPU::PSUBUSW_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(psubusw, v16, v16);
}

void SoftFPU::PMULHW_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(pmulhw, v16, v16);
}
void SoftFPU::PMULLW_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(pmullw, v16, v16);
}

void SoftFPU::PMADDWD_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(pmaddwd, v32, v16);
}

// COMPARISON
void SoftFPU::PCMPEQB_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v8 = mm.v8 == mm64m.v8;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PCMPEQW_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v16 = mm.v16 == mm64m.v16;
    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PCMPEQD_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v32 = mm.v32 == mm64m.v32;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}

void SoftFPU::PCMPGTB_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v8 = mm.v8 > mm64m.v8;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PCMPGTW_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v16 = mm.v16 > mm64m.v16;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PCMPGTD_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v32 = mm.v32 > mm64m.v32;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}

// CONVERSION
void SoftFPU::PACKSSDW_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(packssdw, v16, v32);
}
void SoftFPU::PACKSSWB_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(packsswb, v8, v16);
}
void SoftFPU::PACKUSWB_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(packuswb, v8, v16);
}

// UNPACK
void SoftFPU::PUNPCKHBW_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(punpckhbw, v8, v8);
}

void SoftFPU::PUNPCKHWD_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(punpckhwd, v16, v16);
}
void SoftFPU::PUNPCKHDQ_mm1_mm2m64(const X86::Instruction& insn)
{
    MMX_intrinsic(punpckhdq, v32, v32);
}

void SoftFPU::PUNPCKLBW_mm1_mm2m32(const X86::Instruction& insn)
{
    MMX_intrinsic(punpcklbw, v8, v8);
}
void SoftFPU::PUNPCKLWD_mm1_mm2m32(const X86::Instruction& insn)
{
    MMX_intrinsic(punpcklwd, v16, v16);
}
void SoftFPU::PUNPCKLDQ_mm1_mm2m32(const X86::Instruction& insn)
{
    MMX_intrinsic(punpckldq, v32, v32);
}

// LOGICAL
void SoftFPU::PAND_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.raw &= mm64m.raw;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PANDN_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.raw &= ~mm64m.raw;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::POR_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.raw |= mm64m.raw;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PXOR_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.raw ^= mm64m.raw;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}

// SHIFT
void SoftFPU::PSLLW_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v16 <<= mm64m.v16;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSLLW_mm1_imm8(const X86::Instruction& insn)
{
    u8 imm = insn.imm8();
    MMX mm = mmx_get(insn.modrm().reg());

    mm.v16 <<= imm;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSLLD_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v32 <<= mm64m.v32;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSLLD_mm1_imm8(const X86::Instruction& insn)
{
    u8 imm = insn.imm8();
    MMX mm = mmx_get(insn.modrm().reg());

    mm.v32 <<= imm;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSLLQ_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.raw <<= mm64m.raw;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSLLQ_mm1_imm8(const X86::Instruction& insn)
{
    u8 imm = insn.imm8();
    MMX mm = mmx_get(insn.modrm().reg());

    mm.raw <<= imm;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSRAW_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v16 >>= mm64m.v16;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSRAW_mm1_imm8(const X86::Instruction& insn)
{
    u8 imm = insn.imm8();
    MMX mm = mmx_get(insn.modrm().reg());

    mm.v16 >>= imm;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSRAD_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v32 >>= mm64m.v32;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSRAD_mm1_imm8(const X86::Instruction& insn)
{
    u8 imm = insn.imm8();
    MMX mm = mmx_get(insn.modrm().reg());

    mm.v32 >>= imm;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSRLW_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v16u >>= mm64m.v16u;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSRLW_mm1_imm8(const X86::Instruction& insn)
{
    u8 imm = insn.imm8();
    MMX mm = mmx_get(insn.modrm().reg());

    mm.v16u >>= imm;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSRLD_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.v32u >>= mm64m.v32u;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSRLD_mm1_imm8(const X86::Instruction& insn)
{
    u8 imm = insn.imm8();
    MMX mm = mmx_get(insn.modrm().reg());

    mm.v32u >>= imm;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSRLQ_mm1_mm2m64(const X86::Instruction& insn)
{
    LOAD_MM_MM64M();

    mm.raw >>= mm64m.raw;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}
void SoftFPU::PSRLQ_mm1_imm8(const X86::Instruction& insn)
{
    u8 imm = insn.imm8();
    MMX mm = mmx_get(insn.modrm().reg());

    mm.raw >>= imm;

    mmx_set(insn.modrm().reg(), mm);
    mmx_common();
}

// DATA TRANSFER
void SoftFPU::MOVD_mm1_rm32(const X86::Instruction& insn)
{
    u8 mmx_index = insn.modrm().reg();
    // FIXME:: Shadow Value
    // upper half is zeroed out
    mmx_set(mmx_index, { .raw = insn.modrm().read32(m_cpu, insn).value() });
    mmx_common();
};
void SoftFPU::MOVD_rm32_mm2(const X86::Instruction& insn)
{
    u8 mmx_index = insn.modrm().reg();
    // FIXME:: Shadow Value
    insn.modrm().write32(m_cpu, insn,
        shadow_wrap_as_initialized(static_cast<u32>(mmx_get(mmx_index).raw)));
    mmx_common();
};

void SoftFPU::MOVQ_mm1_mm2m64(const X86::Instruction& insn)
{
    // FIXME: Shadow Value
    if (insn.modrm().mod() == 0b11) {
        // instruction
        mmx_set(insn.modrm().reg(),
            mmx_get(insn.modrm().rm()));
    } else {
        mmx_set(insn.modrm().reg(),
            { .raw = insn.modrm().read64(m_cpu, insn).value() });
    }
    mmx_common();
}
void SoftFPU::MOVQ_mm1m64_mm2(const X86::Instruction& insn)
{
    if (insn.modrm().mod() == 0b11) {
        // instruction
        mmx_set(insn.modrm().rm(),
            mmx_get(insn.modrm().reg()));
    } else {
        // FIXME: Shadow Value
        insn.modrm().write64(m_cpu, insn,
            shadow_wrap_as_initialized(mmx_get(insn.modrm().reg()).raw));
    }
    mmx_common();
}
void SoftFPU::MOVQ_mm1_rm64(const X86::Instruction&) { TODO_INSN(); }; // long mode
void SoftFPU::MOVQ_rm64_mm2(const X86::Instruction&) { TODO_INSN(); }; // long mode

// EMPTY MMX STATE
void SoftFPU::EMMS(const X86::Instruction&)
{
    // clear tagword
    m_fpu_tw = 0xFFFF;
}

}
