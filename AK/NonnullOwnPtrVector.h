#pragma once

#include <AK/NonnullPtrVector.h>
#include <AK/NonnullOwnPtr.h>

namespace AK {

template<typename T, int inline_capacity = 0>
class NonnullOwnPtrVector : public NonnullPtrVector<NonnullOwnPtr<T>, inline_capacity>
{
};

}

using AK::NonnullOwnPtrVector;
