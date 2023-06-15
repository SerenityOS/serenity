/*
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/URL.h>

namespace Unicode {

ErrorOr<URL> create_unicode_url(String const&);

}
