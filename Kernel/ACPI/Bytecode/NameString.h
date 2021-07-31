/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/KString.h>

namespace Kernel::ACPI {

class NameString : public RefCounted<NameString> {
public:
    enum class Speciality {
        HasRootChar,
        HasPrefixPath,
        None
    };

    enum class NameMultiplyPrefix {
        Dual,
        Multiple,
        None,
    };

public:
    static RefPtr<NameString> try_to_evaluate_with_validation(Span<u8 const> encoded_strings);
    static RefPtr<NameString> try_to_create(Span<u8 const> encoded_strings);
    static RefPtr<NameString> try_to_create_with_string_view(StringView name_segment);
    const Vector<OwnPtr<KString>>& name_segments() const { return m_name_segments; }

    Speciality speciality() const { return m_speciality; }
    size_t prefix_paths_count() const { return m_speciality == Speciality::HasPrefixPath ? m_prefix_paths_count : 0; }

    String full_name() const;
    size_t encoded_length() const;

private:
    NameString(const Vector<StringView>&, Speciality speciality, NameMultiplyPrefix multiply_prefix, size_t prefix_paths_count);
    NameString(Speciality speciality, size_t prefix_paths_count);
    explicit NameString(StringView name_segment);

    Vector<OwnPtr<KString>> m_name_segments;
    Speciality m_speciality { Speciality::None };
    NameMultiplyPrefix m_prefix { NameMultiplyPrefix::None };
    size_t m_prefix_paths_count { 0 };
};

}
