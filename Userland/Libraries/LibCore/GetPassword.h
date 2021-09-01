/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OSError.h>
#include <AK/Result.h>
#include <AK/String.h>

namespace Core {

Result<String, OSError> get_password(const StringView& prompt = "Password: ");

}
