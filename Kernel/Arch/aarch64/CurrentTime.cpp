/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/CurrentTime.h>

namespace Kernel {

fptr optional_current_time()
{
    return nullptr;
}

}
