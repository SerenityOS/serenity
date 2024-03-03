/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/PlatformDriver.h>

namespace Kernel {

static Singleton<SpinlockProtected<PlatformDriver::List, LockRank::None>> s_all_instances;

SpinlockProtected<PlatformDriver::List, LockRank::None>& PlatformDriver::all_instances()
{
    return s_all_instances;
}

}
