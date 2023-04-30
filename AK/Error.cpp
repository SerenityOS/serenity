/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
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

bool Error::operator==(int code) const
{
    if (!m_code.has<int>())
        return false;

    return m_code.get<int>() == code;
}

bool Error::operator==(CustomError custom_error) const
{
    if (!m_code.has<CustomError>())
        return false;

    return m_code.get<CustomError>() == custom_error;
}

StringView Error::custom_error_as_string() const
{
    switch (m_code.get<CustomError>()) {
    case CustomError::NotEnoughData:
        return "Not enough data to read value"sv;
    }

    VERIFY_NOT_REACHED();
}

}
