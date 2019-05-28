#pragma once

#include "Assertions.h"
#include "StdLibExtras.h"

namespace AK {

template<class T>
constexpr auto call_will_be_destroyed_if_present(T* object) -> decltype(object->will_be_destroyed(), TrueType {})
{
    object->will_be_destroyed();
    return {};
}

constexpr auto call_will_be_destroyed_if_present(...) -> FalseType
{
    return {};
}

template<class T>
constexpr auto call_one_retain_left_if_present(T* object) -> decltype(object->one_retain_left(), TrueType {})
{
    object->one_retain_left();
    return {};
}

constexpr auto call_one_retain_left_if_present(...) -> FalseType
{
    return {};
}

class RetainableBase {
public:
    void retain()
    {
        ASSERT(m_retain_count);
        ++m_retain_count;
    }

    int retain_count() const
    {
        return m_retain_count;
    }

protected:
    RetainableBase() {}
    ~RetainableBase()
    {
        ASSERT(!m_retain_count);
    }

    void release_base()
    {
        ASSERT(m_retain_count);
        --m_retain_count;
    }

    int m_retain_count { 1 };
};

template<typename T>
class Retainable : public RetainableBase {
public:
    void release()
    {
        release_base();
        if (m_retain_count == 0) {
            call_will_be_destroyed_if_present(static_cast<T*>(this));
            delete static_cast<T*>(this);
        } else if (m_retain_count == 1) {
            call_one_retain_left_if_present(static_cast<T*>(this));
        }
    }
};

}

using AK::Retainable;
