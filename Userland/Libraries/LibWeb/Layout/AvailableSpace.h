/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Format.h>
#include <LibWeb/Forward.h>

namespace Web::Layout {

class AvailableSize {
public:
    enum class Type {
        Definite,
        Indefinite,
        MinContent,
        MaxContent,
    };

    static AvailableSize make_definite(float);
    static AvailableSize make_indefinite();
    static AvailableSize make_min_content();
    static AvailableSize make_max_content();

    bool is_definite() const { return m_type == Type::Definite; }
    bool is_indefinite() const { return m_type == Type::Indefinite; }
    bool is_min_content() const { return m_type == Type::MinContent; }
    bool is_max_content() const { return m_type == Type::MaxContent; }
    bool is_intrinsic_sizing_constraint() const { return is_min_content() || is_max_content(); }

    float to_px() const
    {
        return m_value;
    }

    float to_px_or_zero() const
    {
        if (!is_definite())
            return 0.0f;
        return m_value;
    }

    DeprecatedString to_string() const;

private:
    AvailableSize(Type type, float);

    Type m_type {};
    float m_value {};
};

class AvailableSpace {
public:
    AvailableSpace(AvailableSize w, AvailableSize h)
        : width(move(w))
        , height(move(h))
    {
    }

    AvailableSize width;
    AvailableSize height;

    DeprecatedString to_string() const;
};

}

template<>
struct AK::Formatter<Web::Layout::AvailableSize> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::Layout::AvailableSize const& available_size)
    {
        return Formatter<StringView>::format(builder, available_size.to_string());
    }
};

template<>
struct AK::Formatter<Web::Layout::AvailableSpace> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::Layout::AvailableSpace const& available_space)
    {
        return Formatter<StringView>::format(builder, available_space.to_string());
    }
};
