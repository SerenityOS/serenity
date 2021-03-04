/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/Format.h>
#include <Kernel/KSyms.h>
#include <Kernel/Panic.h>
#include <Kernel/UBSanitizer.h>

using namespace Kernel;
using namespace Kernel::UBSanitizer;

bool Kernel::UBSanitizer::g_ubsan_is_deadly { true };

extern "C" {

static void print_location(const SourceLocation& location)
{
    if (!location.filename()) {
        dbgln("KUBSAN: in unknown file");
    } else {
        dbgln("KUBSAN: at {}, line {}, column: {}", location.filename(), location.line(), location.column());
    }
    dump_backtrace();
    if (g_ubsan_is_deadly)
        PANIC("UB is configured to be deadly.");
}

void __ubsan_handle_load_invalid_value(const InvalidValueData&, ValueHandle);
void __ubsan_handle_load_invalid_value(const InvalidValueData& data, ValueHandle)
{
    dbgln("KUBSAN: load-invalid-value: {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_nonnull_arg(const NonnullArgData&);
void __ubsan_handle_nonnull_arg(const NonnullArgData& data)
{
    dbgln("KUBSAN: null pointer passed as argument {}, which is declared to never be null", data.argument_index);
    print_location(data.location);
}

void __ubsan_handle_nullability_arg(const NonnullArgData&);
void __ubsan_handle_nullability_arg(const NonnullArgData& data)
{
    dbgln("KUBSAN: null pointer passed as argument {}, which is declared to never be null", data.argument_index);
    print_location(data.location);
}

void __ubsan_handle_nonnull_return_v1(const NonnullReturnData&, const SourceLocation&);
void __ubsan_handle_nonnull_return_v1(const NonnullReturnData&, const SourceLocation& location)
{
    dbgln("KUBSAN: null pointer return from function declared to never return null");
    print_location(location);
}

void __ubsan_handle_nullability_return_v1(const NonnullReturnData& data, const SourceLocation& location);
void __ubsan_handle_nullability_return_v1(const NonnullReturnData&, const SourceLocation& location)
{
    dbgln("KUBSAN: null pointer return from function declared to never return null");
    print_location(location);
}

void __ubsan_handle_vla_bound_not_positive(const VLABoundData&, ValueHandle);
void __ubsan_handle_vla_bound_not_positive(const VLABoundData& data, ValueHandle)
{
    dbgln("KUBSAN: VLA bound not positive {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_add_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs);
void __ubsan_handle_add_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    dbgln("KUBSAN: addition overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());

    print_location(data.location);
}

void __ubsan_handle_sub_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs);
void __ubsan_handle_sub_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    dbgln("KUBSAN: subtraction overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());

    print_location(data.location);
}

void __ubsan_handle_negate_overflow(const OverflowData&, ValueHandle);
void __ubsan_handle_negate_overflow(const OverflowData& data, ValueHandle)
{

    dbgln("KUBSAN: negation overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());

    print_location(data.location);
}

void __ubsan_handle_mul_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs);
void __ubsan_handle_mul_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    dbgln("KUBSAN: multiplication overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_shift_out_of_bounds(const ShiftOutOfBoundsData&, ValueHandle lhs, ValueHandle rhs);
void __ubsan_handle_shift_out_of_bounds(const ShiftOutOfBoundsData& data, ValueHandle, ValueHandle)
{
    dbgln("KUBSAN: shift out of bounds, {} ({}-bit) shifted by {} ({}-bit)", data.lhs_type.name(), data.lhs_type.bit_width(), data.rhs_type.name(), data.rhs_type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_divrem_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs);
void __ubsan_handle_divrem_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    dbgln("KUBSAN: divrem overlow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_out_of_bounds(const OutOfBoundsData&, ValueHandle);
void __ubsan_handle_out_of_bounds(const OutOfBoundsData& data, ValueHandle)
{
    dbgln("KUBSAN: out of bounds access into array of {} ({}-bit), index type {} ({}-bit)", data.array_type.name(), data.array_type.bit_width(), data.index_type.name(), data.index_type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_type_mismatch_v1(const TypeMismatchData&, ValueHandle);
void __ubsan_handle_type_mismatch_v1(const TypeMismatchData& data, ValueHandle ptr)
{
    static const char* kinds[] = {
        "load of",
        "store to",
        "reference binding to",
        "member access within",
        "member call on",
        "constructor call on",
        "downcast of",
        "downcast of",
        "upcast of",
        "cast to virtual base of",
        "_Nonnull binding to",
        "dynamic operation on"
    };

    FlatPtr alignment = (FlatPtr)1 << data.log_alignment;
    auto* kind = kinds[data.type_check_kind];

    if (!ptr) {
        dbgln("KUBSAN: {} null pointer of type {}", kind, data.type.name());
    } else if ((FlatPtr)ptr & (alignment - 1)) {
        dbgln("KUBSAN: {} misaligned address {:p} of type {}", kind, ptr, data.type.name());
    } else {
        dbgln("KUBSAN: {} address {:p} with insufficient space for type {}", kind, ptr, data.type.name());
    }

    print_location(data.location);
}

// FIXME: Causes a triple fault on boot
void __ubsan_handle_alignment_assumption(const AlignmentAssumptionData&, ValueHandle, ValueHandle, ValueHandle);
void __ubsan_handle_alignment_assumption(const AlignmentAssumptionData& data, ValueHandle pointer, ValueHandle alignment, ValueHandle offset)
{
    if (offset) {
        dbgln(
            "KUBSAN: assumption of {:p} byte alignment (with offset of {:p} byte) for pointer {:p}"
            "of type {} failed",
            alignment, offset, pointer, data.type.name());
    } else {
        dbgln("KUBSAN: assumption of {:p} byte alignment for pointer {:p}"
              "of type {} failed",
            alignment, pointer, data.type.name());
    }
    // dbgln("KUBSAN: Assumption of pointer allignment failed");
    print_location(data.location);
}

void __ubsan_handle_builtin_unreachable(const UnreachableData&);
void __ubsan_handle_builtin_unreachable(const UnreachableData& data)
{
    dbgln("KUBSAN: execution reached an unreachable program point");
    print_location(data.location);
}

void __ubsan_handle_missing_return(const UnreachableData&);
void __ubsan_handle_missing_return(const UnreachableData& data)
{
    dbgln("KUBSAN: execution reached the end of a value-returning function without returning a value");
    print_location(data.location);
}

void __ubsan_handle_implicit_conversion(const ImplicitConversionData&, ValueHandle, ValueHandle);
void __ubsan_handle_implicit_conversion(const ImplicitConversionData& data, ValueHandle, ValueHandle)
{
    const char* src_signed = data.from_type.is_signed() ? "" : "un";
    const char* dst_signed = data.to_type.is_signed() ? "" : "un";
    dbgln("KUBSAN: implicit conversion from type {} ({}-bit, {}signed) to type {} ({}-bit, {}signed)",
        data.from_type.name(), data.from_type.bit_width(), src_signed, data.to_type.name(), data.to_type.bit_width(), dst_signed);
    print_location(data.location);
}

void __ubsan_handle_invalid_builtin(const InvalidBuiltinData);
void __ubsan_handle_invalid_builtin(const InvalidBuiltinData data)
{
    dbgln("KUBSAN: passing invalid argument");
    print_location(data.location);
}

// FIXME: Causes a triple fault on boot
void __ubsan_handle_pointer_overflow(const PointerOverflowData&, ValueHandle, ValueHandle);
void __ubsan_handle_pointer_overflow(const PointerOverflowData& data, ValueHandle base, ValueHandle result)
{
    if (base == 0 && result == 0) {
        dbgln("KUBSAN: applied zero offset to nullptr");
    } else if (base == 0 && result != 0) {
        dbgln("KUBSAN: applied non-zero offset {:p} to nullptr", result);
    } else if (base != 0 && result == 0) {
        dbgln("KUBSAN: applying non-zero offset to non-null pointer {:p} produced null pointer", base);
    } else {
        dbgln("KUBSAN: addition of unsigned offset to {:p} overflowed to {:p}", base, result);
    }
    print_location(data.location);
}
}
