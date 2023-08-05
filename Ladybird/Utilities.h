/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Error.h>
#include <AK/String.h>
#include <AK/Vector.h>

void platform_init();
ErrorOr<String> application_directory();
ErrorOr<Vector<String>> get_paths_for_helper_process(StringView process_name);

extern DeprecatedString s_serenity_resource_root;
