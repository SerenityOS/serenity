/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::USB {

// USB 2.0 Specification Section 9.4.6
static constexpr u8 USB_MAX_ADDRESS = 127;

}
