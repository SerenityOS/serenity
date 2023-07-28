/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <LibWeb/CSS/Angle.h>
#include <LibWeb/CSS/Frequency.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/StyleValues/CalculatedStyleValue.h>
#include <LibWeb/CSS/Time.h>

namespace Web::CSS {

template<typename T>
class CalculatedOr {
public:
    CalculatedOr(T t)
        : m_value(move(t))
    {
    }

    CalculatedOr(NonnullRefPtr<CalculatedStyleValue> calculated)
        : m_value(move(calculated))
    {
    }

    virtual ~CalculatedOr() = default;

    bool is_calculated() const { return m_value.template has<NonnullRefPtr<CalculatedStyleValue>>(); }

    T const& value() const
    {
        VERIFY(!is_calculated());
        return m_value.template get<T>();
    }

    NonnullRefPtr<CalculatedStyleValue> const& calculated() const
    {
        VERIFY(is_calculated());
        return m_value.template get<NonnullRefPtr<CalculatedStyleValue>>();
    }

    virtual T resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const&, Layout::Node const&) const = 0;

    T resolved(Layout::Node const& layout_node) const
    {
        return m_value.visit(
            [&](T const& t) {
                return t;
            },
            [&](NonnullRefPtr<CalculatedStyleValue> const& calculated) {
                return resolve_calculated(calculated, layout_node);
            });
    }

    ErrorOr<String> to_string() const
    {
        if (is_calculated())
            return m_value.template get<NonnullRefPtr<CalculatedStyleValue>>()->to_string();

        return m_value.template get<T>().to_string();
    }

    bool operator==(CalculatedOr<T> const& other) const
    {
        if (is_calculated() || other.is_calculated())
            return false;
        return (m_value.template get<T>() == other.m_value.template get<T>());
    }

private:
    Variant<T, NonnullRefPtr<CalculatedStyleValue>> m_value;
};

class AngleOrCalculated : public CalculatedOr<Angle> {
public:
    using CalculatedOr<Angle>::CalculatedOr;

    Angle resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const&, Layout::Node const&) const override;
};

class FrequencyOrCalculated : public CalculatedOr<Frequency> {
public:
    using CalculatedOr<Frequency>::CalculatedOr;

    Frequency resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const&, Layout::Node const&) const override;
};

class LengthOrCalculated : public CalculatedOr<Length> {
public:
    using CalculatedOr<Length>::CalculatedOr;

    Length resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const&, Layout::Node const&) const override;
    [[nodiscard]] Length resolved(Length::ResolutionContext const&) const;
};

class PercentageOrCalculated : public CalculatedOr<Percentage> {
public:
    using CalculatedOr<Percentage>::CalculatedOr;

    Percentage resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const&, Layout::Node const&) const override;
};

class TimeOrCalculated : public CalculatedOr<Time> {
public:
    using CalculatedOr<Time>::CalculatedOr;

    Time resolve_calculated(NonnullRefPtr<CalculatedStyleValue> const&, Layout::Node const&) const override;
};

}

template<>
struct AK::Formatter<Web::CSS::AngleOrCalculated> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::AngleOrCalculated const& calculated_or)
    {
        return Formatter<StringView>::format(builder, TRY(calculated_or.to_string()));
    }
};

template<>
struct AK::Formatter<Web::CSS::FrequencyOrCalculated> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::FrequencyOrCalculated const& calculated_or)
    {
        return Formatter<StringView>::format(builder, TRY(calculated_or.to_string()));
    }
};

template<>
struct AK::Formatter<Web::CSS::LengthOrCalculated> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::LengthOrCalculated const& calculated_or)
    {
        return Formatter<StringView>::format(builder, TRY(calculated_or.to_string()));
    }
};

template<>
struct AK::Formatter<Web::CSS::PercentageOrCalculated> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::PercentageOrCalculated const& calculated_or)
    {
        return Formatter<StringView>::format(builder, TRY(calculated_or.to_string()));
    }
};

template<>
struct AK::Formatter<Web::CSS::TimeOrCalculated> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::TimeOrCalculated const& calculated_or)
    {
        return Formatter<StringView>::format(builder, TRY(calculated_or.to_string()));
    }
};
