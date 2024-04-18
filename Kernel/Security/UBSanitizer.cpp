/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/UBSanitizer.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/KSyms.h>

using namespace Kernel;
using namespace AK::UBSanitizer;

Atomic<bool> AK::UBSanitizer::g_ubsan_is_deadly { true };

extern "C" {

static void print_location(SourceLocation const& location)
{
    if (!location.filename())
        critical_dmesgln("KUBSAN: in unknown file");
    else
        critical_dmesgln("KUBSAN: at {}, line {}, column: {}", location.filename(), location.line(), location.column());
    dump_backtrace(g_ubsan_is_deadly ? PrintToScreen::Yes : PrintToScreen::No);
    if (g_ubsan_is_deadly) {
        critical_dmesgln("UB is configured to be deadly, halting the system.");
        Processor::halt();
    }
}

void __ubsan_handle_load_invalid_value(InvalidValueData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_load_invalid_value(InvalidValueData const& data, ValueHandle)
{
    critical_dmesgln("KUBSAN: load-invalid-value: {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_nonnull_arg(NonnullArgData const&) __attribute__((used));
void __ubsan_handle_nonnull_arg(NonnullArgData const& data)
{
    critical_dmesgln("KUBSAN: null pointer passed as argument {}, which is declared to never be null", data.argument_index);
    print_location(data.location);
}

void __ubsan_handle_nullability_arg(NonnullArgData const&) __attribute__((used));
void __ubsan_handle_nullability_arg(NonnullArgData const& data)
{
    critical_dmesgln("KUBSAN: null pointer passed as argument {}, which is declared to never be null", data.argument_index);
    print_location(data.location);
}

void __ubsan_handle_nonnull_return_v1(NonnullReturnData const&, SourceLocation const&) __attribute__((used));
void __ubsan_handle_nonnull_return_v1(NonnullReturnData const&, SourceLocation const& location)
{
    critical_dmesgln("KUBSAN: null pointer return from function declared to never return null");
    print_location(location);
}

void __ubsan_handle_nullability_return_v1(NonnullReturnData const& data, SourceLocation const& location) __attribute__((used));
void __ubsan_handle_nullability_return_v1(NonnullReturnData const&, SourceLocation const& location)
{
    critical_dmesgln("KUBSAN: null pointer return from function declared to never return null");
    print_location(location);
}

void __ubsan_handle_vla_bound_not_positive(VLABoundData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_vla_bound_not_positive(VLABoundData const& data, ValueHandle)
{
    critical_dmesgln("KUBSAN: VLA bound not positive {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_add_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_add_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    critical_dmesgln("KUBSAN: addition overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());

    print_location(data.location);
}

void __ubsan_handle_sub_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_sub_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    critical_dmesgln("KUBSAN: subtraction overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());

    print_location(data.location);
}

void __ubsan_handle_negate_overflow(OverflowData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_negate_overflow(OverflowData const& data, ValueHandle)
{
    critical_dmesgln("KUBSAN: negation overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());

    print_location(data.location);
}

void __ubsan_handle_mul_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_mul_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    critical_dmesgln("KUBSAN: multiplication overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_shift_out_of_bounds(ShiftOutOfBoundsData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_shift_out_of_bounds(ShiftOutOfBoundsData const& data, ValueHandle, ValueHandle)
{
    critical_dmesgln("KUBSAN: shift out of bounds, {} ({}-bit) shifted by {} ({}-bit)", data.lhs_type.name(), data.lhs_type.bit_width(), data.rhs_type.name(), data.rhs_type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_divrem_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_divrem_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    critical_dmesgln("KUBSAN: divrem overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_out_of_bounds(OutOfBoundsData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_out_of_bounds(OutOfBoundsData const& data, ValueHandle)
{
    critical_dmesgln("KUBSAN: out of bounds access into array of {} ({}-bit), index type {} ({}-bit)", data.array_type.name(), data.array_type.bit_width(), data.index_type.name(), data.index_type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_type_mismatch_v1(TypeMismatchData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_type_mismatch_v1(TypeMismatchData const& data, ValueHandle ptr)
{
    constexpr StringView kinds[] = {
        "load of"sv,
        "store to"sv,
        "reference binding to"sv,
        "member access within"sv,
        "member call on"sv,
        "constructor call on"sv,
        "downcast of"sv,
        "downcast of"sv,
        "upcast of"sv,
        "cast to virtual base of"sv,
        "_Nonnull binding to"sv,
        "dynamic operation on"sv
    };

    FlatPtr alignment = (FlatPtr)1 << data.log_alignment;
    auto kind = kinds[data.type_check_kind];

    if (!ptr)
        critical_dmesgln("KUBSAN: {} null pointer of type {}", kind, data.type.name());
    else if ((FlatPtr)ptr & (alignment - 1))
        critical_dmesgln("KUBSAN: {} misaligned address {:p} of type {}", kind, ptr, data.type.name());
    else
        critical_dmesgln("KUBSAN: {} address {:p} with insufficient space for type {}", kind, ptr, data.type.name());

    print_location(data.location);
}

void __ubsan_handle_alignment_assumption(AlignmentAssumptionData const&, ValueHandle, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_alignment_assumption(AlignmentAssumptionData const& data, ValueHandle pointer, ValueHandle alignment, ValueHandle offset)
{
    if (offset)
        critical_dmesgln("KUBSAN: assumption of {:p} byte alignment (with offset of {:p} byte) for pointer {:p} of type {} failed", alignment, offset, pointer, data.type.name());
    else
        critical_dmesgln("KUBSAN: assumption of {:p} byte alignment for pointer {:p} of type {} failed", alignment, pointer, data.type.name());

    print_location(data.location);
}

void __ubsan_handle_builtin_unreachable(UnreachableData const&) __attribute__((used));
void __ubsan_handle_builtin_unreachable(UnreachableData const& data)
{
    critical_dmesgln("KUBSAN: execution reached an unreachable program point");
    print_location(data.location);
}

void __ubsan_handle_missing_return(UnreachableData const&) __attribute__((used));
void __ubsan_handle_missing_return(UnreachableData const& data)
{
    critical_dmesgln("KUBSAN: execution reached the end of a value-returning function without returning a value");
    print_location(data.location);
}

void __ubsan_handle_implicit_conversion(ImplicitConversionData const&, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_implicit_conversion(ImplicitConversionData const& data, ValueHandle, ValueHandle)
{
    char const* src_signed = data.from_type.is_signed() ? "" : "un";
    char const* dst_signed = data.to_type.is_signed() ? "" : "un";
    critical_dmesgln("KUBSAN: implicit conversion from type {} ({}-bit, {}signed) to type {} ({}-bit, {}signed)", data.from_type.name(), data.from_type.bit_width(), src_signed, data.to_type.name(), data.to_type.bit_width(), dst_signed);
    print_location(data.location);
}

void __ubsan_handle_invalid_builtin(InvalidBuiltinData const) __attribute__((used));
void __ubsan_handle_invalid_builtin(InvalidBuiltinData const data)
{
    critical_dmesgln("KUBSAN: passing invalid argument");
    print_location(data.location);
}

void __ubsan_handle_pointer_overflow(PointerOverflowData const&, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_pointer_overflow(PointerOverflowData const& data, ValueHandle base, ValueHandle result)
{
    if (base == 0 && result == 0)
        critical_dmesgln("KUBSAN: applied zero offset to nullptr");
    else if (base == 0 && result != 0)
        critical_dmesgln("KUBSAN: applied non-zero offset {:p} to nullptr", result);
    else if (base != 0 && result == 0)
        critical_dmesgln("KUBSAN: applying non-zero offset to non-null pointer {:p} produced null pointer", base);
    else
        critical_dmesgln("KUBSAN: addition of unsigned offset to {:p} overflowed to {:p}", base, result);
    print_location(data.location);
}

void __ubsan_handle_function_type_mismatch(FunctionTypeMismatchData const&) __attribute__((used));
void __ubsan_handle_function_type_mismatch(FunctionTypeMismatchData const& data)
{
    critical_dmesgln("KUBSAN: call to function through pointer to incorrect function type {}", data.type.name());
    print_location(data.location);
}
}
