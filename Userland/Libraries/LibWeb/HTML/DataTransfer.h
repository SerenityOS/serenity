/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::HTML {

class DataTransfer : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DataTransfer, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(DataTransfer);

public:
    static JS::NonnullGCPtr<DataTransfer> construct_impl(JS::Realm&);
    virtual ~DataTransfer() override;

private:
    DataTransfer(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
};

}
