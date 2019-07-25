#pragma once

#include <AK/NonnullPtrVector.h>
#include <AK/NonnullRefPtr.h>

namespace AK {

template<typename T, int inline_capacity = 0>
class NonnullRefPtrVector : public NonnullPtrVector<NonnullRefPtr<T>, inline_capacity>
{
};

}

using AK::NonnullRefPtrVector;
