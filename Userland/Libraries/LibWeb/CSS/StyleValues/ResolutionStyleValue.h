/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Resolution.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class ResolutionStyleValue : public StyleValueWithDefaultOperators<ResolutionStyleValue> {
public:
    static ErrorOr<ValueComparingNonnullRefPtr<ResolutionStyleValue>> create(Resolution resolution)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) ResolutionStyleValue(move(resolution)));
    }
    virtual ~ResolutionStyleValue() override = default;

    Resolution const& resolution() const { return m_resolution; }

    virtual ErrorOr<String> to_string() const override { return m_resolution.to_string(); }

    bool properties_equal(ResolutionStyleValue const& other) const { return m_resolution == other.m_resolution; }

private:
    explicit ResolutionStyleValue(Resolution resolution)
        : StyleValueWithDefaultOperators(Type::Resolution)
        , m_resolution(move(resolution))
    {
    }

    Resolution m_resolution;
};

}
