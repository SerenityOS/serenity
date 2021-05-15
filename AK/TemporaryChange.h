/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace AK {

template<typename T>
class TemporaryChange {
public:
    TemporaryChange(T& variable, T value)
        : m_variable(variable)
        , m_old_value(variable)
    {
        m_variable = value;
    }
    ~TemporaryChange() { m_variable = m_old_value; }

private:
    T& m_variable;
    T m_old_value;
};

}

using AK::TemporaryChange;
