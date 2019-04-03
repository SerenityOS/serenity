#pragma once

#include <AK/StdLibExtras.h>

namespace AK {

template<typename T>
class Eternal {
public:
    template<typename... Args>
    Eternal(Args&&... args)
    {
        new (m_slot) T(forward<Args>(args)...);
    }

    T& get() { return *reinterpret_cast<T*>(&m_slot); }
    const T& get() const { return *reinterpret_cast<T*>(&m_slot); }
    T* operator->() { return &get(); }
    const T* operator->() const { return &get(); }
    operator T&() { return get(); }
    operator const T&() const { return get(); }


private:
    [[gnu::aligned(alignof(T))]] char m_slot[sizeof(T)];
};

}

using AK::Eternal;
