/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

class ScriptExecutionContext {
public:
    virtual ~ScriptExecutionContext();

    // FIXME: This should not work this way long-term, interpreters should be on the stack.
    virtual JS::Interpreter& interpreter() = 0;
};

}
