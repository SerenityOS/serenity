/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace WebDriver {

struct WebDriverError {
    unsigned http_status;
    String error;
    String message;
};

}

template<>
struct AK::Formatter<WebDriver::WebDriverError> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, WebDriver::WebDriverError const& error)
    {
        return Formatter<StringView>::format(builder, String::formatted("Error {}, {}: {}", error.http_status, error.error, error.message));
    }
};
