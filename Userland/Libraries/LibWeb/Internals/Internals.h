/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::Internals {

class Internals final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(Internals, Bindings::PlatformObject);

public:
    virtual ~Internals() override;

    void gc();

private:
    explicit Internals(JS::Realm&);
    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
};

}
