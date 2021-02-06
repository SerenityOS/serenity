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
#include <Kernel/UBSanitizer.h>

using namespace Kernel;
using namespace Kernel::UBSanitizer;

extern "C" {

static void print_location(const SourceLocation& location)
{
    dbgln("KUBSAN: at {}, line {}, column: {}", location.filename(), location.line(), location.column());
    dump_backtrace();
}

void __ubsan_handle_load_invalid_value(const InvalidValueData&, void*);
void __ubsan_handle_load_invalid_value(const InvalidValueData& data, void*)
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

void __ubsan_handle_vla_bound_not_positive(const VLABoundData&, void*);
void __ubsan_handle_vla_bound_not_positive(const VLABoundData& data, void*)
{
    dbgln("KUBSAN: VLA bound not positive {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_add_overflow(const OverflowData&, void* lhs, void* rhs);
void __ubsan_handle_add_overflow(const OverflowData& data, void*, void*)
{
    dbgln("KUBSAN: addition overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_sub_overflow(const OverflowData&, void* lhs, void* rhs);
void __ubsan_handle_sub_overflow(const OverflowData& data, void*, void*)
{
    dbgln("KUBSAN: subtraction overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_negate_overflow(const OverflowData&, void*);
void __ubsan_handle_negate_overflow(const OverflowData& data, void*)
{
    dbgln("KUBSAN: negation overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_mul_overflow(const OverflowData&, void* lhs, void* rhs);
void __ubsan_handle_mul_overflow(const OverflowData& data, void*, void*)
{
    dbgln("KUBSAN: multiplication overflow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_shift_out_of_bounds(const ShiftOutOfBoundsData&, void* lhs, void* rhs);
void __ubsan_handle_shift_out_of_bounds(const ShiftOutOfBoundsData& data, void*, void*)
{
    dbgln("KUBSAN: shift out of bounds, {} ({}-bit) shifted by {} ({}-bit)", data.lhs_type.name(), data.lhs_type.bit_width(), data.rhs_type.name(), data.rhs_type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_divrem_overflow(const OverflowData&, void* lhs, void* rhs);
void __ubsan_handle_divrem_overflow(const OverflowData& data, void*, void*)
{
    dbgln("KUBSAN: divrem overlow, {} ({}-bit)", data.type.name(), data.type.bit_width());
    print_location(data.location);
}

void __ubsan_handle_out_of_bounds(const OutOfBoundsData&, void*);
void __ubsan_handle_out_of_bounds(const OutOfBoundsData& data, void*)
{
    dbgln("KUBSAN: out of bounds access into array of {} ({}-bit), index type {} ({}-bit)", data.array_type.name(), data.array_type.bit_width(), data.index_type.name(), data.index_type.bit_width());
    print_location(data.location);
}
}
