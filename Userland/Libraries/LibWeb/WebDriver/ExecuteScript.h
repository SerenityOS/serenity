/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/JsonValue.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Forward.h>

namespace Web::WebDriver {

enum class ExecuteScriptResultType {
    PromiseResolved,
    PromiseRejected,
    Timeout,
    JavaScriptError,
};

struct ExecuteScriptResult {
    ExecuteScriptResultType type;
    JS::Value value;
};

struct ExecuteScriptResultSerialized {
    ExecuteScriptResultType type;
    JsonValue value;
};

ExecuteScriptResultSerialized execute_script(Page& page, DeprecatedString const& body, JS::MarkedVector<JS::Value> arguments, Optional<u64> const& timeout);
ExecuteScriptResultSerialized execute_async_script(Page& page, DeprecatedString const& body, JS::MarkedVector<JS::Value> arguments, Optional<u64> const& timeout);

}
