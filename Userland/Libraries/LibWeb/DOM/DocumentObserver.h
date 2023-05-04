/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/SafeFunction.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

class DocumentObserver final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DocumentObserver, Bindings::PlatformObject);

public:
    JS::SafeFunction<void()> document_became_inactive;

private:
    explicit DocumentObserver(JS::Realm&, DOM::Document&);

    virtual void visit_edges(Cell::Visitor&) override;
    virtual void finalize() override;

    JS::NonnullGCPtr<DOM::Document> m_document;
};

}
