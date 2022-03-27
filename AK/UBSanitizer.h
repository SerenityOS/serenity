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

class UBSourceLocation {
    AK_MAKE_NONCOPYABLE(UBSourceLocation);

public:
    char const* filename() const { return m_filename; }
    u32 line() const { return m_line; }
    u32 column() const { return m_column; }

    // Replace the location information in the .data segment with one that won't be logged in the future
    //   Using this method prevents log spam when sanitizers are not deadly by not logging the exact same
    //   code paths multiple times.
    UBSourceLocation permanently_clear() { return move(*this); }

    bool needs_logging() const { return !(m_filename == nullptr); }

    UBSourceLocation() = default;
    UBSourceLocation(UBSourceLocation&& other)
        : m_filename(other.m_filename)
        , m_line(other.m_line)
        , m_column(other.m_column)
    {
        other = {};
    }

    UBSourceLocation& operator=(UBSourceLocation&& other)
    {
        if (this != &other) {
            m_filename = exchange(other.m_filename, nullptr);
            m_line = exchange(other.m_line, 0);
            m_column = exchange(other.m_column, 0);
        }
        return *this;
    }

private:
    char const* m_filename { nullptr };
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
    char const* name() const { return m_name; }
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
    UBSourceLocation location;
    TypeDescriptor const& type;
};

struct NonnullArgData {
    UBSourceLocation location;
    UBSourceLocation attribute_location;
    int argument_index;
};

struct NonnullReturnData {
    UBSourceLocation attribute_location;
};

struct OverflowData {
    UBSourceLocation location;
    TypeDescriptor const& type;
};

struct VLABoundData {
    UBSourceLocation location;
    TypeDescriptor const& type;
};

struct ShiftOutOfBoundsData {
    UBSourceLocation location;
    TypeDescriptor const& lhs_type;
    TypeDescriptor const& rhs_type;
};

struct OutOfBoundsData {
    UBSourceLocation location;
    TypeDescriptor const& array_type;
    TypeDescriptor const& index_type;
};

struct TypeMismatchData {
    UBSourceLocation location;
    TypeDescriptor const& type;
    u8 log_alignment;
    u8 type_check_kind;
};

struct AlignmentAssumptionData {
    UBSourceLocation location;
    UBSourceLocation assumption_location;
    TypeDescriptor const& type;
};

struct UnreachableData {
    UBSourceLocation location;
};

struct ImplicitConversionData {
    UBSourceLocation location;
    TypeDescriptor const& from_type;
    TypeDescriptor const& to_type;
    /* ImplicitConversionCheckKind */ unsigned char kind;
};

struct InvalidBuiltinData {
    UBSourceLocation location;
    unsigned char kind;
};

struct PointerOverflowData {
    UBSourceLocation location;
};

struct FloatCastOverflowData {
    UBSourceLocation location;
    TypeDescriptor const& from_type;
    TypeDescriptor const& to_type;
};

}
