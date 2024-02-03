/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Types.h>

#include "VFSRootContextLayout.h"

namespace LayoutParsing {

ErrorOr<void> handle_creation_sequence(VFSRootContextLayout&, JsonArray const& layout_creation_sequence);

}
