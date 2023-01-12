/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<ProcessList>> ProcessList::create()
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ProcessList());
}

}
