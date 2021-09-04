/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <errno.h>
#include <string.h>

namespace AK {

class OSError {
public:
    explicit OSError(int error)
        : m_error(error)
    {
    }

    int error() const { return m_error; }
    char const* string() const { return strerror(m_error); }

private:
    int m_error { 0 };
};

template<>
struct Formatter<OSError> : Formatter<StringView> {
    void format(FormatBuilder& builder, OSError const& value)
    {
        Formatter<StringView>::format(builder, value.string());
    }
};

}

using AK::OSError;
