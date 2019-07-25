#pragma once

namespace AK {

template<typename T>
class ScopedValueRollback {
public:
    ScopedValueRollback(T& variable)
        : m_variable(variable)
        , m_saved_value(variable)
    {
    }

    ~ScopedValueRollback()
    {
        m_variable = m_saved_value;
    }

private:
    T& m_variable;
    T m_saved_value;
};

}

using AK::ScopedValueRollback;
