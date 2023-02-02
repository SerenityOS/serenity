/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define AK_DONT_REPLACE_STD

#include <AK/Error.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/StringView.h>
#include <LibCore/System.h>

ErrorOr<void> spawn_helper_process(StringView process_name, Span<StringView> arguments, Core::System::SearchInPath, Optional<Span<StringView>> environment = {});
ErrorOr<Vector<String>> get_paths_for_helper_process(StringView process_name);
