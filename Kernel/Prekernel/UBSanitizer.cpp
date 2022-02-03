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

static void print_location(const SourceLocation&)
{
#if ARCH(I386) || ARCH(X86_64)
    asm volatile("cli; hlt");
#else
    for (;;) { }
#endif
}

void __ubsan_handle_load_invalid_value(const InvalidValueData&, ValueHandle) __attribute__((used));
void __ubsan_handle_load_invalid_value(const InvalidValueData& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_nonnull_arg(const NonnullArgData&) __attribute__((used));
void __ubsan_handle_nonnull_arg(const NonnullArgData& data)
{
    print_location(data.location);
}

void __ubsan_handle_nullability_arg(const NonnullArgData&) __attribute__((used));
void __ubsan_handle_nullability_arg(const NonnullArgData& data)
{
    print_location(data.location);
}

void __ubsan_handle_nonnull_return_v1(const NonnullReturnData&, const SourceLocation&) __attribute__((used));
void __ubsan_handle_nonnull_return_v1(const NonnullReturnData&, const SourceLocation& location)
{
    print_location(location);
}

void __ubsan_handle_nullability_return_v1(const NonnullReturnData& data, const SourceLocation& location) __attribute__((used));
void __ubsan_handle_nullability_return_v1(const NonnullReturnData&, const SourceLocation& location)
{
    print_location(location);
}

void __ubsan_handle_vla_bound_not_positive(const VLABoundData&, ValueHandle) __attribute__((used));
void __ubsan_handle_vla_bound_not_positive(const VLABoundData& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_add_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_add_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_sub_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_sub_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_negate_overflow(const OverflowData&, ValueHandle) __attribute__((used));
void __ubsan_handle_negate_overflow(const OverflowData& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_mul_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_mul_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_shift_out_of_bounds(const ShiftOutOfBoundsData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_shift_out_of_bounds(const ShiftOutOfBoundsData& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_divrem_overflow(const OverflowData&, ValueHandle lhs, ValueHandle rhs) __attribute__((used));
void __ubsan_handle_divrem_overflow(const OverflowData& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_out_of_bounds(const OutOfBoundsData&, ValueHandle) __attribute__((used));
void __ubsan_handle_out_of_bounds(const OutOfBoundsData& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_type_mismatch_v1(const TypeMismatchData&, ValueHandle) __attribute__((used));
void __ubsan_handle_type_mismatch_v1(const TypeMismatchData& data, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_alignment_assumption(const AlignmentAssumptionData&, ValueHandle, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_alignment_assumption(const AlignmentAssumptionData& data, ValueHandle, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_builtin_unreachable(const UnreachableData&) __attribute__((used));
void __ubsan_handle_builtin_unreachable(const UnreachableData& data)
{
    print_location(data.location);
}

void __ubsan_handle_missing_return(const UnreachableData&) __attribute__((used));
void __ubsan_handle_missing_return(const UnreachableData& data)
{
    print_location(data.location);
}

void __ubsan_handle_implicit_conversion(const ImplicitConversionData&, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_implicit_conversion(const ImplicitConversionData& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}

void __ubsan_handle_invalid_builtin(const InvalidBuiltinData) __attribute__((used));
void __ubsan_handle_invalid_builtin(const InvalidBuiltinData data)
{
    print_location(data.location);
}

void __ubsan_handle_pointer_overflow(const PointerOverflowData&, ValueHandle, ValueHandle) __attribute__((used));
void __ubsan_handle_pointer_overflow(const PointerOverflowData& data, ValueHandle, ValueHandle)
{
    print_location(data.location);
}
}
