#pragma once

#include "Assertions.h"
#include "StdLibExtras.h"

namespace AK {

template<class T>
constexpr auto call_will_be_destroyed_if_present(T* object) -> decltype(object->will_be_destroyed(), TrueType { })
{
    object->will_be_destroyed();
    return { };
}

constexpr auto call_will_be_destroyed_if_present(...) -> FalseType
{
    return { };
}

template<typename T>
class Retainable {
public:
    void retain()
    {
        ASSERT(m_retain_count);
        ++m_retain_count;
    }

    void release()
    {
        ASSERT(m_retain_count);
        if (!--m_retain_count) {
            call_will_be_destroyed_if_present(static_cast<T*>(this));
            delete static_cast<T*>(this);
        }
    }

    int retain_count() const
    {
        return m_retain_count;
    }

protected:
    Retainable() { }
    ~Retainable()
    {
        ASSERT(!m_retain_count);
    }

private:
    int m_retain_count { 1 };
};

}

using AK::Retainable;

