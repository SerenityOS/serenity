#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>

namespace AK {

template<typename T, int inline_capacity = 0>
class NonnullRefPtrVector : public Vector<NonnullRefPtr<T>, inline_capacity> {
    typedef Vector<NonnullRefPtr<T>, inline_capacity> Base;

public:
    NonnullRefPtrVector()
    {
    }

    NonnullRefPtrVector(Vector<NonnullRefPtr<T>>&& other)
        : Base(static_cast<Base&&>(other))
    {
    }
    NonnullRefPtrVector(const Vector<NonnullRefPtr<T>>& other)
        : Base(static_cast<const Base&>(other))
    {
    }

    using Base::size;

    using Iterator = VectorIterator<NonnullRefPtrVector, T>;
    Iterator begin() { return Iterator(*this, 0); }
    Iterator end() { return Iterator(*this, size()); }

    using ConstIterator = ConstVectorIterator<NonnullRefPtrVector, T>;
    ConstIterator begin() const { return ConstIterator(*this, 0); }
    ConstIterator end() const { return ConstIterator(*this, size()); }

    T& at(int index) { return *Base::at(index); }
    const T& at(int index) const { return *Base::at(index); }
    T& operator[](int index) { return at(index); }
    const T& operator[](int index) const { return at(index); }
    T& first() { return at(0); }
    const T& first() const { return at(0); }
    T& last() { return at(size() - 1); }
    const T& last() const { return at(size() - 1); }
};

}

using AK::NonnullRefPtrVector;
