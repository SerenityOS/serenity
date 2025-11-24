/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace Kernel::DeviceTree {

class InterruptController {
public:
    virtual ~InterruptController() = default;

    virtual ErrorOr<size_t> translate_interrupt_specifier_to_interrupt_number(ReadonlyBytes) const = 0;
};

}
