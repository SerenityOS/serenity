/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Weakable.h>
#include <LibJS/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

class ScriptExecutionContext {
public:
    virtual ~ScriptExecutionContext();

    virtual JS::Realm& realm() = 0;
};

}
