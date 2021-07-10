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

ALWAYS_INLINE void SoftFPU::warn_if_fpu_not_set_absolute(u8 index) const
{
    if (!fpu_is_set(index)) [[unlikely]] {
        // FIXME: Are we supposed to set a flag here?
        //        We might need to raise a stack underflow here
        reportln("\033[31;1mWarning! Read of uninitialized value on the FPU Stack ({} abs)\033[0m\n", index);
        m_emulator.dump_backtrace();
    }
}
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

ALWAYS_INLINE long double SoftFPU::fpu_get(u8 index) const
{
    VERIFY(index < 8);
    warn_if_fpu_not_set_absolute(index);
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
    // The high bytes are set to 0b11... to make the floatingpoint value NaN.
    // This way we are technically able to find out if we are reading the wrong
    // type, but this is still difficult, so we use our own lookup for that
    // We set the alignment bytes to all 1's, too, just in case
    m_storage[index].__high = ~(decltype(m_storage[index].__high))0u;
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
__attribute__((pure)) ALWAYS_INLINE T SoftFPU::fpu_round(long double value) const
{
    // FIXME: may need to set indefinite values manually
    switch (fpu_get_round_mode()) {
    case RoundingMode::NEAREST:
        return static_cast<T>(roundl(value));
    case RoundingMode::DOWN:
        return static_cast<T>(floorl(value));
    case RoundingMode::UP:
        return static_cast<T>(ceill(value));
    case RoundingMode::TRUNK:
        return static_cast<T>(truncl(value));
    default:
        VERIFY_NOT_REACHED();
    }
}

template<Arithmetic T>
__attribute__((pure)) ALWAYS_INLINE T SoftFPU::fpu_round_checked(long double value)
{
    T result = fpu_round<T>(value);
    if (auto rnd = value - result) {
        if (rnd > 0)
            set_c1(1);
        else
            set_c1(0);
        fpu_set_exception(FPU_Exception::Precision);
    }
    return result;
}

template<FloatingPoint T>
__attribute__((pure)) ALWAYS_INLINE T SoftFPU::fpu_convert(long double value) const
{
    // FIXME: actually round the right way
    return static_cast<T>(value);
}
template<FloatingPoint T>
__attribute__((pure)) ALWAYS_INLINE T SoftFPU::fpu_convert_checked(long double value)
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

template<Signed R, Signed I>
__attribute__((const)) ALWAYS_INLINE R signed_saturate(I input)
{
    if (input > NumericLimits<R>::max())
        return NumericLimits<R>::max();
    if (input < NumericLimits<R>::min())
        return NumericLimits<R>::min();
    return static_cast<R>(input);
}
template<Unsigned R, Unsigned I>
__attribute__((const)) ALWAYS_INLINE R unsigned_saturate(I input)
{
    if (input > NumericLimits<R>::max())
        return NumericLimits<R>::max();
    return static_cast<R>(input);
}

}