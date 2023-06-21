/*
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace Core::Version {

ErrorOr<String> read_long_version_string();

}
