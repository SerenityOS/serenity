/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/String.h>
#include <LibWeb/Forward.h>

namespace Web::Layout {

class AvailableSpace {
public:
    enum class Type {
        Definite,
        Indefinite,
        MinContent,
        MaxContent,
    };

    static AvailableSpace make_definite(float);
    static AvailableSpace make_indefinite();
    static AvailableSpace make_min_content();
    static AvailableSpace make_max_content();

    bool is_definite() const { return m_type == Type::Definite; }
    bool is_indefinite() const { return m_type == Type::Indefinite; }
    bool is_min_content() const { return m_type == Type::MinContent; }
    bool is_max_content() const { return m_type == Type::MaxContent; }
    bool is_intrinsic_sizing_constraint() const { return is_min_content() || is_max_content(); }

    float definite_value() const
    {
        VERIFY(is_definite());
        return m_value;
    }

    float to_px() const
    {
        return m_value;
    }

    String to_string() const;

private:
    AvailableSpace(Type type, float);

    Type m_type {};
    float m_value {};
};

}

template<>
struct AK::Formatter<Web::Layout::AvailableSpace> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::Layout::AvailableSpace const& available_space)
    {
        return Formatter<StringView>::format(builder, available_space.to_string());
    }
};
