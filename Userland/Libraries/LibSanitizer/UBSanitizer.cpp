/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/UBSanitizer.h>

using namespace AK::UBSanitizer;

Atomic<bool> AK::UBSanitizer::g_ubsan_is_deadly;

#define WARNLN_AND_DBGLN(fmt, ...) \
    warnln(fmt, ##__VA_ARGS__);    \
    dbgln("\x1B[31m" fmt "\x1B[0m", ##__VA_ARGS__);

extern "C" {

static void print_location(SourceLocation const& location)
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
        if (!options.is_null()) {
            if (options.contains("halt_on_error=1"))
                g_ubsan_is_deadly = true;
            else if (options.contains("halt_on_error=0"))
                g_ubsan_is_deadly = false;
        }
    }
    if (g_ubsan_is_deadly) {
        WARNLN_AND_DBGLN("UB is configured to be deadly");
        VERIFY_NOT_REACHED();
    }
}

void __ubsan_handle_load_invalid_value(InvalidValueData&, ValueHandle) __attribute__((used));
void __ubsan_handle_load_invalid_value(InvalidValueData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: load-invalid-value: {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}

void __ubsan_handle_nonnull_arg(NonnullArgData&) __attribute__((used));
void __ubsan_handle_nonnull_arg(NonnullArgData& data)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: null pointer passed as argument {}, which is declared to never be null", data.argument_index);
    print_location(location);
}

void __ubsan_handle_nullability_arg(NonnullArgData&) __attribute__((used));
void __ubsan_handle_nullability_arg(NonnullArgData& data)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: null pointer passed as argument {}, which is declared to never be null", data.argument_index);
    print_location(location);
}

void __ubsan_handle_nonnull_return_v1(NonnullReturnData const&, SourceLocation&) __attribute__((used));
void __ubsan_handle_nonnull_return_v1(NonnullReturnData const&, SourceLocation& location)
{
    auto loc = location.permanently_clear();
    if (!loc.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: null pointer return from function declared to never return null");
    print_location(loc);
}

void __ubsan_handle_nullability_return_v1(NonnullReturnData const& data, SourceLocation& location) __attribute__((used));
void __ubsan_handle_nullability_return_v1(NonnullReturnData const&, SourceLocation& location)
{
    auto loc = location.permanently_clear();
    if (!loc.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: null pointer return from function declared to never return null");
    print_location(loc);
}

void __ubsan_handle_vla_bound_not_positive(VLABoundData&, ValueHandle) __attribute__((used));
void __ubsan_handle_vla_bound_not_positive(VLABoundData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: VLA bound not positive {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}

void __ubsan_handle_add_overflow(OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_add_overflow(OverflowData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: addition overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}

void __ubsan_handle_sub_overflow(OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_sub_overflow(OverflowData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: subtraction overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}

void __ubsan_handle_negate_overflow(OverflowData&, ValueHandle) __attribute__((used));
void __ubsan_handle_negate_overflow(OverflowData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: negation overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}

void __ubsan_handle_mul_overflow(OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_mul_overflow(OverflowData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: multiplication overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}

void __ubsan_handle_shift_out_of_bounds(ShiftOutOfBoundsData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_shift_out_of_bounds(ShiftOutOfBoundsData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: shift out of bounds, {} ({}-bit) shifted by {} ({}-bit)", data.lhs_type.name(), data.lhs_type.bit_width(), data.rhs_type.name(), data.rhs_type.bit_width());
    print_location(location);
}

void __ubsan_handle_divrem_overflow(OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_divrem_overflow(OverflowData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: divrem overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}

void __ubsan_handle_out_of_bounds(OutOfBoundsData&, ValueHandle) __attribute__((used));
void __ubsan_handle_out_of_bounds(OutOfBoundsData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: out of bounds access into array of {} ({}-bit), index type {} ({}-bit)", data.array_type.name(), data.array_type.bit_width(), data.index_type.name(), data.index_type.bit_width());
    print_location(location);
}

void __ubsan_handle_type_mismatch_v1(TypeMismatchData&, ValueHandle) __attribute__((used));
void __ubsan_handle_type_mismatch_v1(TypeMismatchData& data, ValueHandle ptr)
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

    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;

    FlatPtr alignment = (FlatPtr)1 << data.log_alignment;
    auto kind = kinds[data.type_check_kind];

    if (!ptr) {
        WARNLN_AND_DBGLN("UBSAN: {} null pointer of type {}", kind, data.type.name());
    } else if ((FlatPtr)ptr & (alignment - 1)) {
        WARNLN_AND_DBGLN("UBSAN: {} misaligned address {:p} of type {} which requires {} byte alignment", kind, ptr, data.type.name(), alignment);
    } else {
        WARNLN_AND_DBGLN("UBSAN: {} address {:p} with insufficient space for type {}", kind, ptr, data.type.name());
    }

    print_location(location);
}

void __ubsan_handle_alignment_assumption(AlignmentAssumptionData&, ValueHandle, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_alignment_assumption(AlignmentAssumptionData& data, ValueHandle pointer, ValueHandle alignment, ValueHandle offset)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
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

    print_location(location);
}

void __ubsan_handle_builtin_unreachable(UnreachableData&) __attribute__((used));
void __ubsan_handle_builtin_unreachable(UnreachableData& data)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: execution reached an unreachable program point");
    print_location(location);
}

void __ubsan_handle_missing_return(UnreachableData&) __attribute__((used));
void __ubsan_handle_missing_return(UnreachableData& data)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: execution reached the end of a value-returning function without returning a value");
    print_location(location);
}

void __ubsan_handle_implicit_conversion(ImplicitConversionData&, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_implicit_conversion(ImplicitConversionData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    char const* src_signed = data.from_type.is_signed() ? "" : "un";
    char const* dst_signed = data.to_type.is_signed() ? "" : "un";
    WARNLN_AND_DBGLN("UBSAN: implicit conversion from type {} ({}-bit, {}signed) to type {} ({}-bit, {}signed)",
        data.from_type.name(), data.from_type.bit_width(), src_signed, data.to_type.name(), data.to_type.bit_width(), dst_signed);
    print_location(location);
}

void __ubsan_handle_invalid_builtin(InvalidBuiltinData&) __attribute__((used));
void __ubsan_handle_invalid_builtin(InvalidBuiltinData& data)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: passing invalid argument");
    print_location(location);
}

void __ubsan_handle_pointer_overflow(PointerOverflowData&, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_pointer_overflow(PointerOverflowData& data, ValueHandle base, ValueHandle result)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    if (base == 0 && result == 0) {
        WARNLN_AND_DBGLN("UBSAN: applied zero offset to nullptr");
    } else if (base == 0 && result != 0) {
        WARNLN_AND_DBGLN("UBSAN: applied non-zero offset {:p} to nullptr", result);
    } else if (base != 0 && result == 0) {
        WARNLN_AND_DBGLN("UBSAN: applying non-zero offset to non-null pointer {:p} produced null pointer", base);
    } else {
        WARNLN_AND_DBGLN("UBSAN: addition of unsigned offset to {:p} overflowed to {:p}", base, result);
    }
    print_location(location);
}

void __ubsan_handle_float_cast_overflow(FloatCastOverflowData&, ValueHandle) __attribute__((used));
void __ubsan_handle_float_cast_overflow(FloatCastOverflowData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: overflow when casting from {} to {}", data.from_type.name(), data.to_type.name());
    print_location(location);
}

} // extern "C"
