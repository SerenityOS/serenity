/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/Noncopyable.h"
#include "AK/StdLibExtras.h"
#include <AK/Atomic.h>
#include <AK/Types.h>

namespace AK::UBSanitizer {

extern Atomic<bool> g_ubsan_is_deadly;

typedef void* ValueHandle;

class SourceLocation {
    AK_MAKE_NONCOPYABLE(SourceLocation);

public:
    const char* filename() const { return m_filename; }
    u32 line() const { return m_line; }
    u32 column() const { return m_column; }

    // Replace the location information in the .data segment with one that won't be logged in the future
    //   Using this method prevents log spam when sanitizers are not deadly by not logging the exact same
    //   code paths multiple times.
    SourceLocation permanently_clear() { return move(*this); }

    bool needs_logging() const { return !(m_filename == nullptr); }

    SourceLocation() = default;
    SourceLocation(SourceLocation&& other)
        : m_filename(other.m_filename)
        , m_line(other.m_line)
        , m_column(other.m_column)
    {
        other = {};
    }

    SourceLocation& operator=(SourceLocation&& other)
    {
        if (this != &other) {
            m_filename = exchange(other.m_filename, nullptr);
            m_line = exchange(other.m_line, 0);
            m_column = exchange(other.m_column, 0);
        }
        return *this;
    }

private:
    const char* m_filename { nullptr };
    u32 m_line { 0 };
    u32 m_column { 0 };
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

struct FloatCastOverflowData {
    SourceLocation location;
    TypeDescriptor const& from_type;
    TypeDescriptor const& to_type;
};

}
