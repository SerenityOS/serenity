#pragma once

namespace AK {

template<typename T>
class ValueRestorer {
public:
    ValueRestorer(T& variable)
        : m_variable(variable)
        , m_saved_value(variable)
    {
    }

    ~ValueRestorer()
    {
        m_variable = m_saved_value;
    }

private:
    T& m_variable;
    T m_saved_value;
};

}

using AK::ValueRestorer;
