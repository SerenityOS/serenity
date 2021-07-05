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

class ProcessorParameter : public Core::Object {
    C_OBJECT_ABSTRACT(ProcessorRangeParameter);
};

class ProcessorBooleanParameter final : public ProcessorParameter {
public:
    ProcessorBooleanParameter(bool initial_value)
        : m_value(initial_value)
    {
    }
    ~ProcessorBooleanParameter();

    bool value() const { return m_value; }
    void set_value(bool value)
    {
        m_value = value;
    }

private:
    bool m_value;
};

class ProcessorRangeParameter final : public ProcessorParameter {
public:
    ProcessorRangeParameter(double min_value, double max_value, double initial_value)
        : m_min_value(min_value)
        , m_max_value(max_value)
        , m_default_value(initial_value)
        , m_value(initial_value)
    {
        VERIFY(initial_value <= max_value && initial_value >= min_value);
    }
    ~ProcessorRangeParameter();

    double min_value() const { return m_min_value; }
    double max_value() const { return m_max_value; }
    double default_value() const { return m_default_value; }
    double value() const { return m_value; }
    void set_value(double value)
    {
        VERIFY(value <= m_max_value && value >= m_min_value);
        m_value = value;
    }

private:
    const double m_min_value;
    const double m_max_value;
    const double m_default_value;
    double m_value;
};

}
