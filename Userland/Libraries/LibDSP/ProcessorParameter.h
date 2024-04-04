/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/FixedPoint.h>
#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibDSP/Music.h>

namespace DSP {

using ParameterFixedPoint = FixedPoint<8, i64>;

// Identifies the different kinds of parameters.
// Note that achieving parameter type identification is NOT possible with typeid().
enum class ParameterType : u8 {
    Invalid = 0,
    Range,
    Enum,
    Boolean,
};

enum class Logarithmic : bool {
    No,
    Yes
};

// Processors have modifiable parameters that should be presented to the UI in a uniform way without requiring the processor itself to implement custom interfaces.
class ProcessorParameter {
public:
    ProcessorParameter(ErrorOr<String> name, ParameterType type)
        : m_type(type)
    {
        if (!name.is_error())
            m_name = name.release_value();
    }

    String const& name() const { return m_name; }
    ParameterType type() const { return m_type; }

private:
    String m_name {};
    ParameterType const m_type;
};

namespace Detail {

struct ProcessorParameterSetValueTag {
    explicit ProcessorParameterSetValueTag() = default;
};

template<typename ParameterT>
class ProcessorParameterSingleValue : public ProcessorParameter {

public:
    ProcessorParameterSingleValue(ErrorOr<String> name, ParameterType type, ParameterT initial_value)
        : ProcessorParameter(move(name), type)
        , m_value(move(initial_value))
    {
    }

    operator ParameterT() const
    {
        return value();
    }

    operator double() const
    requires(IsSame<ParameterT, ParameterFixedPoint>)
    {
        return static_cast<double>(value());
    }

    ParameterT value() const { return m_value; }
    void set_value(ParameterT value)
    {
        set_value_sneaky(value, DSP::Detail::ProcessorParameterSetValueTag {});
        for (auto const& did_change_value : m_change_value_listeners)
            did_change_value(value);
    }

    // Use of this function is discouraged. It doesn't notify the value listener.
    void set_value_sneaky(ParameterT value, [[maybe_unused]] Detail::ProcessorParameterSetValueTag)
    {
        if (value != m_value)
            m_value = value;
    }

    // FIXME: Devise a good API for unregistering listeners.
    void register_change_listener(Function<void(ParameterT const&)> listener)
    {
        m_change_value_listeners.append(move(listener));
    }

protected:
    ParameterT m_value;
    Vector<Function<void(ParameterT const&)>> m_change_value_listeners;
};
}

class ProcessorBooleanParameter final : public Detail::ProcessorParameterSingleValue<bool> {
public:
    ProcessorBooleanParameter(String name, bool initial_value)
        : Detail::ProcessorParameterSingleValue<bool>(move(name), ParameterType::Boolean, move(initial_value))
    {
    }
};

class ProcessorRangeParameter final : public Detail::ProcessorParameterSingleValue<ParameterFixedPoint> {
public:
    ProcessorRangeParameter(ErrorOr<String> name, ParameterFixedPoint min_value, ParameterFixedPoint max_value, ParameterFixedPoint initial_value, Logarithmic logarithmic)
        : Detail::ProcessorParameterSingleValue<ParameterFixedPoint>(move(name), ParameterType::Range, move(initial_value))
        , m_min_value(move(min_value))
        , m_max_value(move(max_value))
        , m_default_value(move(initial_value))
        , m_logarithmic(logarithmic)
    {
        VERIFY(initial_value <= max_value && initial_value >= min_value);
    }

    ProcessorRangeParameter(ProcessorRangeParameter const& to_copy)
        : ProcessorRangeParameter(to_copy.name(), to_copy.min_value(), to_copy.max_value(), to_copy.value(), to_copy.is_logarithmic())
    {
    }

    ParameterFixedPoint min_value() const { return m_min_value; }
    ParameterFixedPoint max_value() const { return m_max_value; }
    ParameterFixedPoint range() const { return m_max_value - m_min_value; }
    constexpr Logarithmic is_logarithmic() const { return m_logarithmic; }
    ParameterFixedPoint default_value() const { return m_default_value; }
    void set_value(ParameterFixedPoint value)
    {
        Detail::ProcessorParameterSingleValue<ParameterFixedPoint>::set_value(value.clamp(min_value(), max_value()));
    }

private:
    double const m_min_value;
    double const m_max_value;
    double const m_default_value;
    Logarithmic const m_logarithmic;
};

template<Enum EnumT>
class ProcessorEnumParameter final : public Detail::ProcessorParameterSingleValue<EnumT> {
public:
    ProcessorEnumParameter(ErrorOr<String> name, EnumT initial_value)
        : Detail::ProcessorParameterSingleValue<EnumT>(move(name), ParameterType::Enum, initial_value)
    {
    }
};

}
template<>
struct AK::Formatter<DSP::ProcessorRangeParameter> : AK::StandardFormatter {

    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }
    ErrorOr<void> format(FormatBuilder& builder, DSP::ProcessorRangeParameter value)
    {
        if (m_mode == Mode::Pointer) {
            Formatter<FlatPtr> formatter { *this };
            return formatter.format(builder, reinterpret_cast<FlatPtr>(&value));
        }

        if (m_sign_mode != FormatBuilder::SignMode::Default)
            VERIFY_NOT_REACHED();
        if (m_alternative_form)
            VERIFY_NOT_REACHED();
        if (m_zero_pad)
            VERIFY_NOT_REACHED();
        if (m_mode != Mode::Default)
            VERIFY_NOT_REACHED();
        if (m_width.has_value() && m_precision.has_value())
            VERIFY_NOT_REACHED();

        m_width = m_width.value_or(0);
        m_precision = m_precision.value_or(NumericLimits<size_t>::max());

        TRY(builder.put_literal(TRY(String::formatted("[{} - {}]: {}", value.min_value(), value.max_value(), value.value()))));
        return {};
    }
};
