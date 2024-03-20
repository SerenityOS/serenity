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

static void print_location(SourceLocation const&)
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

void __ubsan_handle_load_invalid_value(InvalidValueData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_load_invalid_value(InvalidValueData const& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_nonnull_arg(NonnullArgData const&) __attribute__((used));
void __ubsan_handle_nonnull_arg(NonnullArgData const& data)
{
    print_location(data.location);
}

void __ubsan_handle_nullability_arg(NonnullArgData const&) __attribute__((used));
void __ubsan_handle_nullability_arg(NonnullArgData const& data)
{
    print_location(data.location);
}

void __ubsan_handle_nonnull_return_v1(NonnullReturnData const&, SourceLocation const&) __attribute__((used));
void __ubsan_handle_nonnull_return_v1(NonnullReturnData const&, SourceLocation const& location)
{
    print_location(location);
}

void __ubsan_handle_nullability_return_v1(NonnullReturnData const& data, SourceLocation const& location) __attribute__((used));
void __ubsan_handle_nullability_return_v1(NonnullReturnData const&, SourceLocation const& location)
{
    print_location(location);
}

void __ubsan_handle_vla_bound_not_positive(VLABoundData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_vla_bound_not_positive(VLABoundData const& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_add_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_add_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_sub_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_sub_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_negate_overflow(OverflowData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_negate_overflow(OverflowData const& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_mul_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_mul_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_shift_out_of_bounds(ShiftOutOfBoundsData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_shift_out_of_bounds(ShiftOutOfBoundsData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_divrem_overflow(OverflowData const&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_divrem_overflow(OverflowData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_out_of_bounds(OutOfBoundsData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_out_of_bounds(OutOfBoundsData const& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_type_mismatch_v1(TypeMismatchData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_type_mismatch_v1(TypeMismatchData const& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_alignment_assumption(AlignmentAssumptionData const&, ValueHandle, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_alignment_assumption(AlignmentAssumptionData const& data, ValueHandle, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_builtin_unreachable(UnreachableData const&) __attribute__((used));
void __ubsan_handle_builtin_unreachable(UnreachableData const& data)
{
    print_location(data.location);
}

void __ubsan_handle_missing_return(UnreachableData const&) __attribute__((used));
void __ubsan_handle_missing_return(UnreachableData const& data)
{
    print_location(data.location);
}

void __ubsan_handle_implicit_conversion(ImplicitConversionData const&, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_implicit_conversion(ImplicitConversionData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_invalid_builtin(InvalidBuiltinData const) __attribute__((used));
void __ubsan_handle_invalid_builtin(InvalidBuiltinData const data)
{
    print_location(data.location);
}

void __ubsan_handle_pointer_overflow(PointerOverflowData const&, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_pointer_overflow(PointerOverflowData const& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_function_type_mismatch(FunctionTypeMismatchData const&, ValueHandle) __attribute__((used));
void __ubsan_handle_function_type_mismatch(FunctionTypeMismatchData const& data, ValueHandle)
{
    print_location(data.location);
}
}
