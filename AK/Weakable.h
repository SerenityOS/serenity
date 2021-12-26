#pragma once

#include "Assertions.h"
#include "Retainable.h"

namespace AK {

template<typename T> class Weakable;
template<typename T> class WeakPtr;

template<typename T>
class WeakLink : public Retainable<WeakLink<T>> {
    friend class Weakable<T>;
public:
    T* ptr() { return static_cast<T*>(m_ptr); }
    const T* ptr() const { return static_cast<const T*>(m_ptr); }

private:
    explicit WeakLink(Weakable<T>& weakable) : m_ptr(&weakable) { }
    Weakable<T>* m_ptr;
};

template<typename T>
class Weakable {
private:
    class Link;
public:
    WeakPtr<T> makeWeakPtr();

protected:
    Weakable() { }

    ~Weakable()
    {
        if (m_link)
            m_link->m_ptr = nullptr;
    }

private:
    RetainPtr<WeakLink<T>> m_link;
};

}

using AK::Weakable;
