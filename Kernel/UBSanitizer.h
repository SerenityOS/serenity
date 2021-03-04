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

#pragma once

#include <AK/Types.h>

namespace Kernel::UBSanitizer {

extern bool g_ubsan_is_deadly;

typedef void* ValueHandle;

class SourceLocation {
public:
    const char* filename() const { return m_filename; }
    u32 line() const { return m_line; }
    u32 column() const { return m_column; }

private:
    const char* m_filename;
    u32 m_line;
    u32 m_column;
};

enum TypeKind : u16 {
    Integer = 0,
    Float = 1,
    Unknown = 0xffff,
};

class TypeDescriptor {
public:
    const char* name() const { return m_name; }
    TypeKind kind() const { return (TypeKind)m_kind; }
    bool is_integer() const { return kind() == TypeKind::Integer; }
    bool is_signed() const { return m_info & 1; }
    bool is_unsigned() const { return !is_signed(); }
    size_t bit_width() const { return 1 << (m_info >> 1); }

private:
    u16 m_kind;
    u16 m_info;
    char m_name[1];
};

struct InvalidValueData {
    SourceLocation location;
    const TypeDescriptor& type;
};

struct NonnullArgData {
    SourceLocation location;
    SourceLocation attribute_location;
    int argument_index;
};

struct NonnullReturnData {
    SourceLocation attribute_location;
};

struct OverflowData {
    SourceLocation location;
    const TypeDescriptor& type;
};

struct VLABoundData {
    SourceLocation location;
    const TypeDescriptor& type;
};

struct ShiftOutOfBoundsData {
    SourceLocation location;
    const TypeDescriptor& lhs_type;
    const TypeDescriptor& rhs_type;
};

struct OutOfBoundsData {
    SourceLocation location;
    const TypeDescriptor& array_type;
    const TypeDescriptor& index_type;
};

struct TypeMismatchData {
    SourceLocation location;
    const TypeDescriptor& type;
    u8 log_alignment;
    u8 type_check_kind;
};

struct AlignmentAssumptionData {
    SourceLocation location;
    SourceLocation assumption_location;
    const TypeDescriptor& type;
};

struct UnreachableData {
    SourceLocation location;
};

struct ImplicitConversionData {
    SourceLocation location;
    const TypeDescriptor& from_type;
    const TypeDescriptor& to_type;
    /* ImplicitConversionCheckKind */ unsigned char kind;
};

struct InvalidBuiltinData {
    SourceLocation location;
    unsigned char kind;
};

struct PointerOverflowData {
    SourceLocation location;
};

}
