#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Vector.h>

namespace AK {

template<typename T, int inline_capacity = 0>
class NonnullOwnPtrVector : public Vector<NonnullOwnPtr<T>, inline_capacity> {
    typedef Vector<NonnullOwnPtr<T>, inline_capacity> Base;

public:
    NonnullOwnPtrVector()
    {
    }

    NonnullOwnPtrVector(Vector<NonnullOwnPtr<T>>&& other)
        : Base(static_cast<Base&&>(other))
    {
    }
    NonnullOwnPtrVector(const Vector<NonnullOwnPtr<T>>& other)
        : Base(static_cast<const Base&>(other))
    {
    }

    using Base::size;

    using Iterator = VectorIterator<NonnullOwnPtrVector, T>;
    Iterator begin() { return Iterator(*this, 0); }
    Iterator end() { return Iterator(*this, size()); }

    using ConstIterator = VectorIterator<const NonnullOwnPtrVector, const T>;
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

using AK::NonnullOwnPtrVector;
