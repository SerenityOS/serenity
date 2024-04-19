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

#define ABORT_ALWAYS()                                            \
    do {                                                          \
        WARNLN_AND_DBGLN("UBSAN: This error is not recoverable"); \
        abort();                                                  \
    } while (0)

// FIXME: Dump backtrace of this process (with symbols? without symbols?) in case the user wants non-deadly UBSAN
//        Should probably go through the kernel for SC_dump_backtrace, then access the loader's symbol tables
//        rather than going through the symbolizer service?
#define ABORT_IF_DEADLY()                                                    \
    do {                                                                     \
        if (g_ubsan_is_deadly.load(AK::MemoryOrder::memory_order_acquire)) { \
            WARNLN_AND_DBGLN("UBSAN: UB is configured to be deadly");        \
            abort();                                                         \
        }                                                                    \
    } while (0)

extern "C" {

[[gnu::constructor]] static void init_ubsan_options()
{
    auto const* options_ptr = getenv("UBSAN_OPTIONS");
    auto options = options_ptr != NULL ? StringView { options_ptr, strlen(options_ptr) } : StringView {};
    // FIXME: Parse more options and complain about invalid options
    if (!options.is_null()) {
        if (options.contains("halt_on_error=1"sv))
            g_ubsan_is_deadly.store(true, AK::MemoryOrder::memory_order_relaxed);
        else if (options.contains("halt_on_error=0"sv))
            g_ubsan_is_deadly.store(false, AK::MemoryOrder::memory_order_relaxed);
    }
}

static void print_location(SourceLocation const& location)
{
    if (!location.filename()) {
        WARNLN_AND_DBGLN("UBSAN: in unknown file");
    } else {
        WARNLN_AND_DBGLN("UBSAN: at {}, line {}, column: {}", location.filename(), location.line(), location.column());
    }
}

// Calls to these functions are automatically inserted by the compiler,
// so there is no point in declaring them in a header.
#pragma GCC diagnostic ignored "-Wmissing-declarations"

static void handle_load_invalid_value(InvalidValueData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: load-invalid-value: {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_load_invalid_value(InvalidValueData& data, ValueHandle handle)
{
    handle_load_invalid_value(data, handle);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_load_invalid_value_abort(InvalidValueData& data, ValueHandle handle)
{
    handle_load_invalid_value(data, handle);
    ABORT_ALWAYS();
}

static void handle_nonnull_arg(NonnullArgData& data)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: null pointer passed as argument {}, which is declared to never be null", data.argument_index);
    print_location(location);
}

[[gnu::used]] void __ubsan_handle_nonnull_arg(NonnullArgData& data)
{
    handle_nonnull_arg(data);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_nonnull_arg_abort(NonnullArgData& data)
{
    handle_nonnull_arg(data);
    ABORT_ALWAYS();
}

static void handle_nullability_arg(NonnullArgData& data)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: null pointer passed as argument {}, which is declared to never be null", data.argument_index);
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_nullability_arg(NonnullArgData& data)
{
    handle_nullability_arg(data);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_nullability_arg_abort(NonnullArgData& data)
{
    handle_nullability_arg(data);
    ABORT_ALWAYS();
}

static void handle_nonnull_return_v1(NonnullReturnData const&, SourceLocation& location)
{
    auto loc = location.permanently_clear();
    if (!loc.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: null pointer return from function declared to never return null");
    print_location(loc);
}
[[gnu::used]] void __ubsan_handle_nonnull_return_v1(NonnullReturnData const& data, SourceLocation& location)
{
    handle_nonnull_return_v1(data, location);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_nonnull_return_v1_abort(NonnullReturnData const& data, SourceLocation& location)
{
    handle_nonnull_return_v1(data, location);
    ABORT_ALWAYS();
}

static void handle_nullability_return_v1(NonnullReturnData const&, SourceLocation& location)
{
    auto loc = location.permanently_clear();
    if (!loc.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: null pointer return from function declared to never return null");
    print_location(loc);
}
[[gnu::used]] void __ubsan_handle_nullability_return_v1(NonnullReturnData const& data, SourceLocation& location)
{
    handle_nullability_return_v1(data, location);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_nullability_return_v1_abort(NonnullReturnData const& data, SourceLocation& location)
{
    handle_nullability_return_v1(data, location);
    ABORT_ALWAYS();
}

static void handle_vla_bound_not_positive(VLABoundData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: VLA bound not positive {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_vla_bound_not_positive(VLABoundData& data, ValueHandle handle)
{
    handle_vla_bound_not_positive(data, handle);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_vla_bound_not_positive_abort(VLABoundData& data, ValueHandle handle)
{
    handle_vla_bound_not_positive(data, handle);
    ABORT_ALWAYS();
}

static void handle_add_overflow(OverflowData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: addition overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_add_overflow(OverflowData& data, ValueHandle lhs, ValueHandle rhs)
{
    handle_add_overflow(data, lhs, rhs);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_add_overflow_abort(OverflowData& data, ValueHandle lhs, ValueHandle rhs)
{
    handle_add_overflow(data, lhs, rhs);
    ABORT_ALWAYS();
}

static void handle_sub_overflow(OverflowData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: subtraction overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_sub_overflow(OverflowData& data, ValueHandle lhs, ValueHandle rhs)
{
    handle_sub_overflow(data, lhs, rhs);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_sub_overflow_abort(OverflowData& data, ValueHandle lhs, ValueHandle rhs)
{
    handle_sub_overflow(data, lhs, rhs);
    ABORT_ALWAYS();
}

static void handle_negate_overflow(OverflowData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: negation overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_negate_overflow(OverflowData& data, ValueHandle value)
{
    handle_negate_overflow(data, value);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_negate_overflow_abort(OverflowData& data, ValueHandle value)
{
    handle_negate_overflow(data, value);
    ABORT_ALWAYS();
}

static void handle_mul_overflow(OverflowData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: multiplication overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_mul_overflow(OverflowData& data, ValueHandle lhs, ValueHandle rhs)
{
    handle_mul_overflow(data, lhs, rhs);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_mul_overflow_abort(OverflowData& data, ValueHandle lhs, ValueHandle rhs)
{
    handle_mul_overflow(data, lhs, rhs);
    ABORT_ALWAYS();
}

static void handle_shift_out_of_bounds(ShiftOutOfBoundsData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: shift out of bounds, {} ({}-bit) shifted by {} ({}-bit)", data.lhs_type.name(), data.lhs_type.bit_width(), data.rhs_type.name(), data.rhs_type.bit_width());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_shift_out_of_bounds(ShiftOutOfBoundsData& data, ValueHandle lhs, ValueHandle rhs)
{
    handle_shift_out_of_bounds(data, lhs, rhs);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_shift_out_of_bounds_abort(ShiftOutOfBoundsData& data, ValueHandle lhs, ValueHandle rhs)
{
    handle_shift_out_of_bounds(data, lhs, rhs);
    ABORT_ALWAYS();
}

static void handle_divrem_overflow(OverflowData& data, ValueHandle, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: divrem overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_divrem_overflow(OverflowData& data, ValueHandle lhs, ValueHandle rhs)
{
    handle_divrem_overflow(data, lhs, rhs);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_divrem_overflow_abort(OverflowData& data, ValueHandle lhs, ValueHandle rhs)
{
    handle_divrem_overflow(data, lhs, rhs);
    ABORT_ALWAYS();
}

static void handle_out_of_bounds(OutOfBoundsData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: out of bounds access into array of {} ({}-bit), index type {} ({}-bit)", data.array_type.name(), data.array_type.bit_width(), data.index_type.name(), data.index_type.bit_width());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_out_of_bounds(OutOfBoundsData& data, ValueHandle index)
{
    handle_out_of_bounds(data, index);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_out_of_bounds_abort(OutOfBoundsData& data, ValueHandle index)
{
    handle_out_of_bounds(data, index);
    ABORT_ALWAYS();
}

static void handle_type_mismatch_v1(TypeMismatchData& data, ValueHandle ptr)
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
[[gnu::used]] void __ubsan_handle_type_mismatch_v1(TypeMismatchData& data, ValueHandle ptr)
{
    handle_type_mismatch_v1(data, ptr);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_type_mismatch_v1_abort(TypeMismatchData& data, ValueHandle ptr)
{
    handle_type_mismatch_v1(data, ptr);
    ABORT_ALWAYS();
}

static void handle_alignment_assumption(AlignmentAssumptionData& data, ValueHandle pointer, ValueHandle alignment, ValueHandle offset)
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
[[gnu::used]] void __ubsan_handle_alignment_assumption(AlignmentAssumptionData& data, ValueHandle pointer, ValueHandle alignment, ValueHandle offset)
{
    handle_alignment_assumption(data, pointer, alignment, offset);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_alignment_assumption_abort(AlignmentAssumptionData& data, ValueHandle pointer, ValueHandle alignment, ValueHandle offset)
{
    handle_alignment_assumption(data, pointer, alignment, offset);
    ABORT_ALWAYS();
}

[[gnu::used, noreturn]] void __ubsan_handle_builtin_unreachable(UnreachableData& data)
{
    WARNLN_AND_DBGLN("UBSAN: execution reached an unreachable program point");
    print_location(data.location);
    ABORT_ALWAYS();
}

[[gnu::used, noreturn]] void __ubsan_handle_missing_return(UnreachableData& data)
{
    WARNLN_AND_DBGLN("UBSAN: execution reached the end of a value-returning function without returning a value");
    print_location(data.location);
    ABORT_ALWAYS();
}

static void handle_implicit_conversion(ImplicitConversionData& data, ValueHandle, ValueHandle)
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
[[gnu::used]] void __ubsan_handle_implicit_conversion(ImplicitConversionData& data, ValueHandle from, ValueHandle to)
{
    handle_implicit_conversion(data, from, to);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_implicit_conversion_abort(ImplicitConversionData& data, ValueHandle from, ValueHandle to)
{
    handle_implicit_conversion(data, from, to);
    ABORT_ALWAYS();
}

static void handle_invalid_builtin(InvalidBuiltinData& data)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: passing invalid argument");
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_invalid_builtin(InvalidBuiltinData& data)
{
    handle_invalid_builtin(data);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_invalid_builtin_abort(InvalidBuiltinData& data)
{
    handle_invalid_builtin(data);
    ABORT_ALWAYS();
}

static void handle_pointer_overflow(PointerOverflowData& data, ValueHandle base, ValueHandle result)
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
[[gnu::used]] void __ubsan_handle_pointer_overflow(PointerOverflowData& data, ValueHandle base, ValueHandle result)
{
    handle_pointer_overflow(data, base, result);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_pointer_overflow_abort(PointerOverflowData& data, ValueHandle base, ValueHandle result)
{
    handle_pointer_overflow(data, base, result);
    ABORT_ALWAYS();
}

static void handle_float_cast_overflow(FloatCastOverflowData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: overflow when casting from {} to {}", data.from_type.name(), data.to_type.name());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_float_cast_overflow(FloatCastOverflowData& data, ValueHandle value)
{
    handle_float_cast_overflow(data, value);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_float_cast_overflow_abort(FloatCastOverflowData& data, ValueHandle value)
{
    handle_float_cast_overflow(data, value);
    ABORT_ALWAYS();
}

static void handle_function_type_mismatch(FunctionTypeMismatchData& data, ValueHandle)
{
    auto location = data.location.permanently_clear();
    if (!location.needs_logging())
        return;
    WARNLN_AND_DBGLN("UBSAN: call to function through pointer to incorrect function type {}", data.type.name());
    print_location(location);
}
[[gnu::used]] void __ubsan_handle_function_type_mismatch(FunctionTypeMismatchData& data, ValueHandle value)
{
    handle_function_type_mismatch(data, value);
    ABORT_IF_DEADLY();
}
[[gnu::used, noreturn]] void __ubsan_handle_function_type_mismatch_abort(FunctionTypeMismatchData& data, ValueHandle value)
{
    handle_function_type_mismatch(data, value);
    ABORT_ALWAYS();
}
} // extern "C"
