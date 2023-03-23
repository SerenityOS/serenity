/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Error.h"

namespace PreviewServer {

AK::Error from_preview_server_error(Error preview_error)
{
    switch (preview_error) {
    case Error::FileNotFound:
        return AK::Error::from_errno(ENOENT);
    case Error::PreviewCreationError:
        return AK::Error::from_string_view("Preview creation failed"sv);
    case Error::OutOfMemory:
        return AK::Error::from_errno(ENOMEM);
    }
    return AK::Error::from_string_view("Unknown error"sv);
}

Error from_generic_error(AK::Error const& error)
{
    if (error.is_errno()) {
        switch (error.code()) {
        case ENOMEM:
            return Error::OutOfMemory;
        case EIO:
        case EFAULT:
        case ENFILE:
        case ENOSPC:
        case EMFILE:
        case EISDIR:
        case ENOENT:
        case EACCES:
            return Error::FileNotFound;
        }
    }
    // Any unknown error number or string error is most likely a custom error from the preview generator.
    return Error::PreviewCreationError;
}

}
