/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>

namespace System {

ErrorOr<void> pledge(StringView promises, StringView execpromises);
ErrorOr<void> unveil(StringView path, StringView permissions);

}
