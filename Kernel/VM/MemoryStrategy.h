#pragma once

namespace Kernel {

enum class AllocationStrategy {
    Reserve = 0,
    AllocateNow,
    None
};

}
