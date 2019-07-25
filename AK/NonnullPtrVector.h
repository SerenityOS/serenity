#pragma once

#include <AK/Vector.h>

namespace AK {

template<typename PtrType, int inline_capacity = 0>
class NonnullPtrVector : public Vector<PtrType, inline_capacity> {
    typedef typename PtrType::ElementType T;
    typedef Vector<PtrType, inline_capacity> Base;

public:
    NonnullPtrVector()
    {
    }

    NonnullPtrVector(Vector<PtrType>&& other)
        : Base(static_cast<Base&&>(other))
    {
    }
    NonnullPtrVector(const Vector<PtrType>& other)
        : Base(static_cast<const Base&>(other))
    {
    }

    using Base::size;

    using Iterator = VectorIterator<NonnullPtrVector, T>;
    Iterator begin() { return Iterator(*this, 0); }
    Iterator end() { return Iterator(*this, size()); }

    using ConstIterator = VectorIterator<const NonnullPtrVector, const T>;
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

private:
    // NOTE: You can't use resize() on a NonnullFooPtrVector since making the vector
    //       bigger would require being able to default-construct NonnullFooPtrs.
    //       Instead, use shrink(new_size).
    void resize(int) = delete;
};

}
