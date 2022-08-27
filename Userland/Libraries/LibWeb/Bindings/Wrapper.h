/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Weakable.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

class Wrapper
    : public PlatformObject
    , public Weakable<Wrapper> {
    JS_OBJECT(Wrapper, PlatformObject);

public:
    virtual ~Wrapper() override;

protected:
    explicit Wrapper(Object& prototype);
};

}
