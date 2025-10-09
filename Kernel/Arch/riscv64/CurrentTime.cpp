/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
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
