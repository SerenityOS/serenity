/*
 * Copyright (c) 2023, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibDiff/Forward.h>

namespace Diff {

ErrorOr<void> apply_patch(Stream& out, Vector<StringView> const& lines, Patch const& patch);

}
