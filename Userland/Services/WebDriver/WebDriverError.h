/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace WebDriver {

// https://w3c.github.io/webdriver/#dfn-error-code
enum class ErrorCode {
    ElementClickIntercepted,
    ElementNotInteractable,
    InsecureCertificate,
    InvalidArgument,
    InvalidCookieDomain,
    InvalidElementState,
    InvalidSelector,
    InvalidSessionId,
    JavascriptError,
    MoveTargetOutOfBounds,
    NoSuchAlert,
    NoSuchCookie,
    NoSuchElement,
    NoSuchFrame,
    NoSuchWindow,
    NoSuchShadowRoot,
    ScriptTimeoutError,
    SessionNotCreated,
    StaleElementReference,
    DetachedShadowRoot,
    Timeout,
    UnableToSetCookie,
    UnableToCaptureScreen,
    UnexpectedAlertOpen,
    UnknownCommand,
    UnknownError,
    UnknownMethod,
    UnsupportedOperation,
};

// https://w3c.github.io/webdriver/#errors
struct WebDriverError {
    unsigned http_status;
    String error;
    String message;

    static WebDriverError from_code(ErrorCode, String message);
};

}

template<>
struct AK::Formatter<WebDriver::WebDriverError> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, WebDriver::WebDriverError const& error)
    {
        return Formatter<StringView>::format(builder, String::formatted("Error {}, {}: {}", error.http_status, error.error, error.message));
    }
};
