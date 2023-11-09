/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>

#ifdef KERNEL
#    include <AK/Format.h>
#endif

namespace AK {

Error Error::from_string_view_or_print_error_and_return_errno(StringView string_literal, [[maybe_unused]] int code)
{
#ifdef KERNEL
    dmesgln("{}", string_literal);
    return Error::from_errno(code);
#else
    return Error::from_string_view(string_literal);
#endif
}

#if defined(AK_OS_WINDOWS)
Error Error::from_windows_error(DWORD code)
{
    char* message = nullptr;

    auto size = FormatMessageA(
        FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
        nullptr,
        code,
        MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
        (LPSTR)&message,
        0,
        nullptr);

    if (size == 0)
        return Error::from_string_view_or_print_error_and_return_errno("Unknown error"sv, code);
    auto error = Error::from_string_view({ message, size });
    LocalFree(message);
    return error;
}
#endif

}
