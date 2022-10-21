/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebDriverError.h"
#include <AK/Vector.h>

namespace WebDriver {

struct ErrorCodeData {
    ErrorCode error_code;
    unsigned http_status;
    String json_error_code;
};

// https://w3c.github.io/webdriver/#dfn-error-code
static Vector<ErrorCodeData> const s_error_code_data = {
    { ErrorCode::ElementClickIntercepted, 400, "element click intercepted" },
    { ErrorCode::ElementNotInteractable, 400, "element not interactable" },
    { ErrorCode::InsecureCertificate, 400, "insecure certificate" },
    { ErrorCode::InvalidArgument, 400, "invalid argument" },
    { ErrorCode::InvalidCookieDomain, 400, "invalid cookie domain" },
    { ErrorCode::InvalidElementState, 400, "invalid element state" },
    { ErrorCode::InvalidSelector, 400, "invalid selector" },
    { ErrorCode::InvalidSessionId, 404, "invalid session id" },
    { ErrorCode::JavascriptError, 500, "javascript error" },
    { ErrorCode::MoveTargetOutOfBounds, 500, "move target out of bounds" },
    { ErrorCode::NoSuchAlert, 404, "no such alert" },
    { ErrorCode::NoSuchCookie, 404, "no such cookie" },
    { ErrorCode::NoSuchElement, 404, "no such element" },
    { ErrorCode::NoSuchFrame, 404, "no such frame" },
    { ErrorCode::NoSuchWindow, 404, "no such window" },
    { ErrorCode::NoSuchShadowRoot, 404, "no such shadow root" },
    { ErrorCode::ScriptTimeoutError, 500, "script timeout" },
    { ErrorCode::SessionNotCreated, 500, "session not created" },
    { ErrorCode::StaleElementReference, 404, "stale element reference" },
    { ErrorCode::DetachedShadowRoot, 404, "detached shadow root" },
    { ErrorCode::Timeout, 500, "timeout" },
    { ErrorCode::UnableToSetCookie, 500, "unable to set cookie" },
    { ErrorCode::UnableToCaptureScreen, 500, "unable to capture screen" },
    { ErrorCode::UnexpectedAlertOpen, 500, "unexpected alert open" },
    { ErrorCode::UnknownCommand, 404, "unknown command" },
    { ErrorCode::UnknownError, 500, "unknown error" },
    { ErrorCode::UnknownMethod, 405, "unknown method" },
    { ErrorCode::UnsupportedOperation, 500, "unsupported operation" },
};

WebDriverError WebDriverError::from_code(ErrorCode code, String message)
{
    auto& data = s_error_code_data[to_underlying(code)];
    return {
        .http_status = data.http_status,
        .error = data.json_error_code,
        .message = move(message)
    };
}

}
