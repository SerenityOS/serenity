/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Definitions.h"

namespace ACPI {

bool validate_table(::ACPI::Structures::SDTHeader const& header);

}
