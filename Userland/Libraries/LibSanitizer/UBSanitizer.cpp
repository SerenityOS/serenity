/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/UBSanitizer.h>

using namespace AK::UBSanitizer;

// FIXME: Parse option from UBSAN_OPTIONS: halt_on_error=0 or 1
bool AK::UBSanitizer::g_ubsan_is_deadly { false };

#define WARNLN_AND_DBGLN(fmt, ...) \
    warnln(fmt, ##__VA_ARGS__);    \
    dbgln(fmt, ##__VA_ARGS__);

extern "C" {

static void print_location(const SourceLocation& location)
{
    if (!location.filename()) {
        WARNLN_AND_DBGLN("UBSAN: in unknown file");
    } else {
        WARNLN_AND_DBGLN("UBSAN: at {}, line {}, column: {}", location.filename(), location.line(), location.column());
    }
    // FIXME: Dump backtrace of this process (with symbols? without symbols?) in case the user wants non-deadly UBSAN
    //    Should probably go through the kernel for SC_dump_backtrace, then access the loader's symbol tables rather than
    //    going through the symbolizer service?

    static bool checked_env_for_deadly = false;
    if (!checked_env_for_deadly) {
        checked_env_for_deadly = true;
        StringView options = getenv("UBSAN_OPTIONS");
        // FIXME: Parse more options and complain about invalid options
        if (!options.is_null() && options.contains("halt_on_error=1"))
            g_ubsan_is_deadly = true;
    }
    if (g_ubsan_is_deadly) {
        WARNLN_AND_DBGLN("UB is configured to be deadly");
        VERIFY_NOT_REACHED();
    }
}

void __ubsan_handle_load_invalid_value(const InvalidValueData&, ValueHandle) __attribute__((used));
void __ubsan_handle_load_invalid_value(const InvalidValueData& data, ValueHandle)
{
    WARNLN_AND_DBGLN("UBSAN: load-invalid-value: {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_nonnull_arg(const NonnullArgData&) __attribute__((used));
void __ubsan_handle_nonnull_arg(const NonnullArgData& data)
{
    WARNLN_AND_DBGLN("UBSAN: null pointer passed as argument {}, which is declared to never be null", data.argument_index);
    print_location(data.location);
}

void __ubsan_handle_nullability_arg(const NonnullArgData&) __attribute__((used));
void __ubsan_handle_nullability_arg(const NonnullArgData& data)
{
    WARNLN_AND_DBGLN("UBSAN: null pointer passed as argument {}, which is declared to never be null", data.argument_index);
    print_location(data.location);
}

void __ubsan_handle_nonnull_return_v1(const NonnullReturnData&, const SourceLocation&) __attribute__((used));
void __ubsan_handle_nonnull_return_v1(const NonnullReturnData&, const SourceLocation& location)
{
    WARNLN_AND_DBGLN("UBSAN: null pointer return from function declared to never return null");
    print_location(location);
}

void __ubsan_handle_nullability_return_v1(const NonnullReturnData& data, const SourceLocation& location) __attribute__((used));
void __ubsan_handle_nullability_return_v1(const NonnullReturnData&, const SourceLocation& location)
{
    WARNLN_AND_DBGLN("UBSAN: null pointer return from function declared to never return null");
    print_location(location);
}

void __ubsan_handle_vla_bound_not_positive(const VLABoundData&, ValueHandle) __attribute__((used));
void __ubsan_handle_vla_bound_not_positive(const VLABoundData& data, ValueHandle)
{
    WARNLN_AND_DBGLN("UBSAN: VLA bound not positive {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_add_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_add_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    WARNLN_AND_DBGLN("UBSAN: addition overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());

    print_location(data.location);
}

void __ubsan_handle_sub_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_sub_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    WARNLN_AND_DBGLN("UBSAN: subtraction overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());

    print_location(data.location);
}

void __ubsan_handle_negate_overflow(const OverflowData&, ValueHandle) __attribute__((used));
void __ubsan_handle_negate_overflow(const OverflowData& data, ValueHandle)
{
    WARNLN_AND_DBGLN("UBSAN: negation overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());

    print_location(data.location);
}

void __ubsan_handle_mul_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_mul_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    WARNLN_AND_DBGLN("UBSAN: multiplication overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_shift_out_of_bounds(const ShiftOutOfBoundsData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_shift_out_of_bounds(const ShiftOutOfBoundsData& data, ValueHandle, ValueHandle)
{
    WARNLN_AND_DBGLN("UBSAN: shift out of bounds, {} ({}-bit) shifted by {} ({}-bit)", data.lhs_type.name(), data.lhs_type.bit_width(), data.rhs_type.name(), data.rhs_type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_divrem_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_divrem_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    WARNLN_AND_DBGLN("UBSAN: divrem overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_out_of_bounds(const OutOfBoundsData&, ValueHandle) __attribute__((used));
void __ubsan_handle_out_of_bounds(const OutOfBoundsData& data, ValueHandle)
{
    WARNLN_AND_DBGLN("UBSAN: out of bounds access into array of {} ({}-bit), index type {} ({}-bit)", data.array_type.name(), data.array_type.bit_width(), data.index_type.name(), data.index_type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_type_mismatch_v1(const TypeMismatchData&, ValueHandle) __attribute__((used));
void __ubsan_handle_type_mismatch_v1(const TypeMismatchData& data, ValueHandle ptr)
{
    constexpr StringView kinds[] = {
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
    auto kind = kinds[data.type_check_kind];

    if (!ptr) {
        WARNLN_AND_DBGLN("UBSAN: {} null pointer of type {}", kind, data.type.name());
    } else if ((FlatPtr)ptr & (alignment - 1)) {
        WARNLN_AND_DBGLN("UBSAN: {} misaligned address {:p} of type {}", kind, ptr, data.type.name());
    } else {
        WARNLN_AND_DBGLN("UBSAN: {} address {:p} with insufficient space for type {}", kind, ptr, data.type.name());
    }

    print_location(data.location);
}

void __ubsan_handle_alignment_assumption(const AlignmentAssumptionData&, ValueHandle, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_alignment_assumption(const AlignmentAssumptionData& data, ValueHandle pointer, ValueHandle alignment, ValueHandle offset)
{
    if (offset) {
        WARNLN_AND_DBGLN(
            "UBSAN: assumption of {:p} byte alignment (with offset of {:p} byte) for pointer {:p}"
            "of type {} failed",
            alignment, offset, pointer, data.type.name());
    } else {
        WARNLN_AND_DBGLN("UBSAN: assumption of {:p} byte alignment for pointer {:p}"
                         "of type {} failed",
            alignment, pointer, data.type.name());
    }

    print_location(data.location);
}

void __ubsan_handle_builtin_unreachable(const UnreachableData&) __attribute__((used));
void __ubsan_handle_builtin_unreachable(const UnreachableData& data)
{
    WARNLN_AND_DBGLN("UBSAN: execution reached an unreachable program point");
    print_location(data.location);
}

void __ubsan_handle_missing_return(const UnreachableData&) __attribute__((used));
void __ubsan_handle_missing_return(const UnreachableData& data)
{
    WARNLN_AND_DBGLN("UBSAN: execution reached the end of a value-returning function without returning a value");
    print_location(data.location);
}

void __ubsan_handle_implicit_conversion(const ImplicitConversionData&, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_implicit_conversion(const ImplicitConversionData& data, ValueHandle, ValueHandle)
{
    const char* src_signed = data.from_type.is_signed() ? "" : "un";
    const char* dst_signed = data.to_type.is_signed() ? "" : "un";
    WARNLN_AND_DBGLN("UBSAN: implicit conversion from type {} ({}-bit, {}signed) to type {} ({}-bit, {}signed)",
        data.from_type.name(), data.from_type.bit_width(), src_signed, data.to_type.name(), data.to_type.bit_width(), dst_signed);
    print_location(data.location);
}

void __ubsan_handle_invalid_builtin(const InvalidBuiltinData) __attribute__((used));
void __ubsan_handle_invalid_builtin(const InvalidBuiltinData data)
{
    WARNLN_AND_DBGLN("UBSAN: passing invalid argument");
    print_location(data.location);
}

void __ubsan_handle_pointer_overflow(const PointerOverflowData&, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_pointer_overflow(const PointerOverflowData& data, ValueHandle base, ValueHandle result)
{
    if (base == 0 && result == 0) {
        WARNLN_AND_DBGLN("UBSAN: applied zero offset to nullptr");
    } else if (base == 0 && result != 0) {
        WARNLN_AND_DBGLN("UBSAN: applied non-zero offset {:p} to nullptr", result);
    } else if (base != 0 && result == 0) {
        WARNLN_AND_DBGLN("UBSAN: applying non-zero offset to non-null pointer {:p} produced null pointer", base);
    } else {
        WARNLN_AND_DBGLN("UBSAN: addition of unsigned offset to {:p} overflowed to {:p}", base, result);
    }
    print_location(data.location);
}

void __ubsan_handle_float_cast_overflow(const FloatCastOverflowData&, ValueHandle) __attribute__((used));
void __ubsan_handle_float_cast_overflow(const FloatCastOverflowData& data, ValueHandle)
{
    WARNLN_AND_DBGLN("UBSAN: overflow when casting from {} to {}", data.from_type.name(), data.to_type.name());
    print_location(data.location);
}
}
