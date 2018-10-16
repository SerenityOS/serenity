#pragma once

#include "Assertions.h"
#include "VGA.h"

#define DEBUG_REFCOUNTED

class RefCountedBase {

protected:
    bool derefBase() const
    {
        return !--m_refCount;
    }
    mutable size_t m_refCount { 1 };
#ifdef DEBUG_REFCOUNTED
    //mutable bool m_adopted { false };
#endif
};

template<typename T>
class RefCounted : public RefCountedBase {
public:
    size_t refCount() const { return m_refCount; }

    void ref() const
    {
#ifdef DEBUG_REFCOUNTED
        ASSERT(m_refCount);
        //ASSERT(m_adopted);
#endif
        ++m_refCount;
    }

    void deref() const
    {
#ifdef DEBUG_REFCOUNTED
        ASSERT(m_refCount);
        //ASSERT(m_adopted);
#endif
        if (derefBase())
            delete static_cast<const T*>(this);
    }

protected:
    RefCounted() { }
    ~RefCounted() { }
};
