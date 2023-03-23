/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>

namespace PreviewServer {

// IPC-compatible simplified error class.
enum class Error {
    PreviewCreationError = 0,
    FileNotFound,
    OutOfMemory,
};

AK::Error from_preview_server_error(Error);
Error from_generic_error(AK::Error const&);

}
