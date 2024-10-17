/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::USB {

enum class SCSIDataDirection {
    DataToTarget,
    DataToInitiator,
    NoData
};

}
