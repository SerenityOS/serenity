/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <AK/Types.h>
#include <LibCore/Object.h>

namespace LibDSP {

// Processors have modifiable parameters that should be presented to the UI in a uniform way without requiring the
class ProcessorParameter {
};

namespace Detail {

template<typename ParameterT>
class ProcessorParameterSingleValue : public ProcessorParameter {

public:
    ProcessorParameterSingleValue(ParameterT initial_value)
        : m_value(initial_value)
    {
    }

    ~ProcessorParameterSingleValue() requires(IsDestructible<ParameterT>)
    {
        m_value.~ParameterT();
    }

    ParameterT value() const { return m_value; };
    void set_value(ParameterT value) { m_value = value; }

protected:
    ParameterT m_value;
};
}

class ProcessorBooleanParameter final : public Detail::ProcessorParameterSingleValue<bool> {
public:
    ProcessorBooleanParameter(bool initial_value)
        : Detail::ProcessorParameterSingleValue<bool>(initial_value)
    {
    }
};

class ProcessorRangeParameter final : public Detail::ProcessorParameterSingleValue<double> {
public:
    ProcessorRangeParameter(double min_value, double max_value, double initial_value)
        : Detail::ProcessorParameterSingleValue<double>(initial_value)
        , m_min_value(min_value)
        , m_max_value(max_value)
        , m_default_value(initial_value)
    {
        VERIFY(initial_value <= max_value && initial_value >= min_value);
    }
    ~ProcessorRangeParameter();

    double min_value() const { return m_min_value; }
    double max_value() const { return m_max_value; }
    double default_value() const { return m_default_value; }
    void set_value(double value)
    {
        VERIFY(value <= m_max_value && value >= m_min_value);
        Detail::ProcessorParameterSingleValue<double>::set_value(value);
    }

private:
    double const m_min_value;
    double const m_max_value;
    double const m_default_value;
};

}
