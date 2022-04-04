/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Forward.h>

#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/RegisterState.h>

namespace Kernel {

void handle_crash(Kernel::RegisterState const&, char const*, int, bool)
{
    VERIFY_NOT_REACHED();
}

}
