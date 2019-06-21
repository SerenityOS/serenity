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
constexpr auto call_one_ref_left_if_present(T* object) -> decltype(object->one_ref_left(), TrueType {})
{
    object->one_ref_left();
    return {};
}

constexpr auto call_one_ref_left_if_present(...) -> FalseType
{
    return {};
}

class RefCountedBase {
public:
    void ref()
    {
        ASSERT(m_ref_count);
        ++m_ref_count;
    }

    int ref_count() const
    {
        return m_ref_count;
    }

protected:
    RefCountedBase() {}
    ~RefCountedBase()
    {
        ASSERT(!m_ref_count);
    }

    void deref_base()
    {
        ASSERT(m_ref_count);
        --m_ref_count;
    }

    int m_ref_count { 1 };
};

template<typename T>
class RefCounted : public RefCountedBase {
public:
    void deref()
    {
        deref_base();
        if (m_ref_count == 0) {
            call_will_be_destroyed_if_present(static_cast<T*>(this));
            delete static_cast<T*>(this);
        } else if (m_ref_count == 1) {
            call_one_ref_left_if_present(static_cast<T*>(this));
        }
    }
};

}

using AK::RefCounted;
