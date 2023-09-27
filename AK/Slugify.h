/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace AK {
ErrorOr<String> slugify(String const& input, char glue = '-');
}

#if USING_AK_GLOBALLY
using AK::slugify;
#endif
