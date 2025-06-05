/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/UBSanitizer.h>

using namespace AK::UBSanitizer;

Atomic<bool> AK::UBSanitizer::g_ubsan_is_deadly { true };

extern "C" {

__attribute__((noreturn)) static void print_location(SourceLocation const&)
{
    for (;;) {
#if ARCH(AARCH64)
        asm volatile("msr daifset, #2; wfi");
#elif ARCH(RISCV64)
        asm volatile("csrw sie, zero; wfi");
#elif ARCH(X86_64)
        asm volatile("cli; hlt");
#else
#    error Unknown architecture
#endif
    }
}

void __ubsan_handle_load_invalid_value(InvalidValueData const&, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_load_invalid_value(InvalidValueData const& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_nonnull_arg(NonnullArgData const&) __attribute__((used, noreturn));
void __ubsan_handle_nonnull_arg(NonnullArgData const& data)
{
    print_location(data.location);
}

void __ubsan_handle_nullability_arg(NonnullArgData const&) __attribute__((used, noreturn));
void __ubsan_handle_nullability_arg(NonnullArgData const& data)
{
    print_location(data.location);
}

void __ubsan_handle_nonnull_return_v1(NonnullReturnData const&, SourceLocation const&) __attribute__((used, noreturn));
void __ubsan_handle_nonnull_return_v1(NonnullReturnData const&, SourceLocation const& location)
{
    print_location(location);
}

void __ubsan_handle_nullability_return_v1(NonnullReturnData const& data, SourceLocation const& location) __attribute__((used, noreturn));
void __ubsan_handle_nullability_return_v1(NonnullReturnData const&, SourceLocation const& location)
{
    print_location(location);
}

void __ubsan_handle_vla_bound_not_positive(VLABoundData const&, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_vla_bound_not_positive(VLABoundData const& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_add_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used, noreturn));
void __ubsan_handle_add_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_sub_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used, noreturn));
void __ubsan_handle_sub_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_negate_overflow(OverflowData const&, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_negate_overflow(OverflowData const& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_mul_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used, noreturn));
void __ubsan_handle_mul_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_shift_out_of_bounds(ShiftOutOfBoundsData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used, noreturn));
void __ubsan_handle_shift_out_of_bounds(ShiftOutOfBoundsData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_divrem_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used, noreturn));
void __ubsan_handle_divrem_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_out_of_bounds(OutOfBoundsData const&, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_out_of_bounds(OutOfBoundsData const& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_type_mismatch_v1(TypeMismatchData const&, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_type_mismatch_v1(TypeMismatchData const& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_alignment_assumption(AlignmentAssumptionData const&, ValueHandle, ValueHandle, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_alignment_assumption(AlignmentAssumptionData const& data, ValueHandle, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_builtin_unreachable(UnreachableData const&) __attribute__((used, noreturn));
void __ubsan_handle_builtin_unreachable(UnreachableData const& data)
{
    print_location(data.location);
}

void __ubsan_handle_missing_return(UnreachableData const&) __attribute__((used, noreturn));
void __ubsan_handle_missing_return(UnreachableData const& data)
{
    print_location(data.location);
}

void __ubsan_handle_implicit_conversion(ImplicitConversionData const&, ValueHandle, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_implicit_conversion(ImplicitConversionData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_invalid_builtin(InvalidBuiltinData const&) __attribute__((used, noreturn));
void __ubsan_handle_invalid_builtin(InvalidBuiltinData const& data)
{
    print_location(data.location);
}

void __ubsan_handle_pointer_overflow(PointerOverflowData const&, ValueHandle, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_pointer_overflow(PointerOverflowData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_function_type_mismatch(FunctionTypeMismatchData const&) __attribute__((used, noreturn));
void __ubsan_handle_function_type_mismatch(FunctionTypeMismatchData const& data)
{
    print_location(data.location);
}

void __ubsan_handle_load_invalid_value_abort(InvalidValueData const&, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_load_invalid_value_abort(InvalidValueData const& val, ValueHandle handle) { __ubsan_handle_load_invalid_value(val, handle); }

void __ubsan_handle_nonnull_arg_abort(NonnullArgData const&) __attribute__((used, noreturn));
void __ubsan_handle_nonnull_arg_abort(NonnullArgData const& arg) { __ubsan_handle_nonnull_arg(arg); }

void __ubsan_handle_nullability_arg_abort(NonnullArgData const&) __attribute__((used, noreturn));
void __ubsan_handle_nullability_arg_abort(NonnullArgData const& arg) { __ubsan_handle_nullability_arg(arg); }

void __ubsan_handle_nonnull_return_v1_abort(NonnullReturnData const&, SourceLocation const&) __attribute__((used, noreturn));
void __ubsan_handle_nonnull_return_v1_abort(NonnullReturnData const& data, SourceLocation const& location) { __ubsan_handle_nonnull_return_v1(data, location); }

void __ubsan_handle_nullability_return_v1_abort(NonnullReturnData const& data, SourceLocation const& location) __attribute__((used, noreturn));
void __ubsan_handle_nullability_return_v1_abort(NonnullReturnData const& data, SourceLocation const& location) { __ubsan_handle_nullability_return_v1(data, location); }

void __ubsan_handle_vla_bound_not_positive_abort(VLABoundData const&, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_vla_bound_not_positive_abort(VLABoundData const& data, ValueHandle handle) { __ubsan_handle_vla_bound_not_positive(data, handle); }

void __ubsan_handle_add_overflow_abort(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used, noreturn));
void __ubsan_handle_add_overflow_abort(OverflowData const& data, ValueHandle lhs, ValueHandle rhs) { __ubsan_handle_add_overflow(data, lhs, rhs); }

void __ubsan_handle_sub_overflow_abort(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used, noreturn));
void __ubsan_handle_sub_overflow_abort(OverflowData const& data, ValueHandle lhs, ValueHandle rhs) { __ubsan_handle_sub_overflow(data, lhs, rhs); }

void __ubsan_handle_negate_overflow_abort(OverflowData const&, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_negate_overflow_abort(OverflowData const& data, ValueHandle value) { __ubsan_handle_negate_overflow(data, value); }

void __ubsan_handle_mul_overflow_abort(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used, noreturn));
void __ubsan_handle_mul_overflow_abort(OverflowData const& data, ValueHandle lhs, ValueHandle rhs) { __ubsan_handle_mul_overflow(data, lhs, rhs); }

void __ubsan_handle_shift_out_of_bounds_abort(ShiftOutOfBoundsData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used, noreturn));
void __ubsan_handle_shift_out_of_bounds_abort(ShiftOutOfBoundsData const& data, ValueHandle lhs, ValueHandle rhs) { __ubsan_handle_shift_out_of_bounds(data, lhs, rhs); }

void __ubsan_handle_divrem_overflow_abort(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used, noreturn));
void __ubsan_handle_divrem_overflow_abort(OverflowData const& data, ValueHandle lhs, ValueHandle rhs) { __ubsan_handle_divrem_overflow(data, lhs, rhs); }

void __ubsan_handle_out_of_bounds_abort(OutOfBoundsData const&, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_out_of_bounds_abort(OutOfBoundsData const& data, ValueHandle value) { __ubsan_handle_out_of_bounds(data, value); }

void __ubsan_handle_type_mismatch_v1_abort(TypeMismatchData const&, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_type_mismatch_v1_abort(TypeMismatchData const& data, ValueHandle value) { __ubsan_handle_type_mismatch_v1(data, value); }

void __ubsan_handle_alignment_assumption_abort(AlignmentAssumptionData const&, ValueHandle, ValueHandle, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_alignment_assumption_abort(AlignmentAssumptionData const& data, ValueHandle pointer, ValueHandle alignment, ValueHandle offset) { __ubsan_handle_alignment_assumption(data, pointer, alignment, offset); }

void __ubsan_handle_builtin_unreachable_abort(UnreachableData const&) __attribute__((used, noreturn));
void __ubsan_handle_builtin_unreachable_abort(UnreachableData const& data) { __ubsan_handle_builtin_unreachable(data); }

void __ubsan_handle_missing_return_abort(UnreachableData const&) __attribute__((used, noreturn));
void __ubsan_handle_missing_return_abort(UnreachableData const& data) { __ubsan_handle_missing_return(data); }

void __ubsan_handle_implicit_conversion_abort(ImplicitConversionData const&, ValueHandle, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_implicit_conversion_abort(ImplicitConversionData const& data, ValueHandle from, ValueHandle to) { __ubsan_handle_implicit_conversion(data, from, to); }

void __ubsan_handle_invalid_builtin_abort(InvalidBuiltinData const&) __attribute__((used, noreturn));
void __ubsan_handle_invalid_builtin_abort(InvalidBuiltinData const& data) { __ubsan_handle_invalid_builtin(data); }

void __ubsan_handle_pointer_overflow_abort(PointerOverflowData const&, ValueHandle, ValueHandle) __attribute__((used, noreturn));
void __ubsan_handle_pointer_overflow_abort(PointerOverflowData const& data, ValueHandle base, ValueHandle result) { __ubsan_handle_pointer_overflow(data, base, result); }

void __ubsan_handle_function_type_mismatch_abort(FunctionTypeMismatchData const&) __attribute__((used, noreturn));
void __ubsan_handle_function_type_mismatch_abort(FunctionTypeMismatchData const& data) { __ubsan_handle_function_type_mismatch(data); }
}
