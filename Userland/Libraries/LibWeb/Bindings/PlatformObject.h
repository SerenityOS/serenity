/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace Web::Bindings {

// https://webidl.spec.whatwg.org/#dfn-platform-object
class PlatformObject : public JS::Object {
    JS_OBJECT(PlatformObject, JS::Object);

public:
    virtual ~PlatformObject() override;

protected:
    explicit PlatformObject(JS::Object& prototype);
};

}
