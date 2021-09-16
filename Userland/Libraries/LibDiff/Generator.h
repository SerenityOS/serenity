/*
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Hunks.h"

namespace Diff {

Vector<Hunk> from_text(StringView const& old_text, StringView const& new_text);

}
