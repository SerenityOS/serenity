/*
 * Copyright (c) 2023, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/DOMStringMap.h>
#include <LibWeb/HTML/GlobalEventHandlers.h>

namespace Web::MathML {

class MathMLElement : public DOM::Element
    , public HTML::GlobalEventHandlers {
    WEB_PLATFORM_OBJECT(MathMLElement, DOM::Element);
    JS_DECLARE_ALLOCATOR(MathMLElement);

public:
    virtual ~MathMLElement() override;

    [[nodiscard]] JS::NonnullGCPtr<HTML::DOMStringMap> dataset();

    virtual Optional<ARIA::Role> default_role() const override;

    void focus();
    void blur();

protected:
    virtual JS::GCPtr<DOM::EventTarget> global_event_handlers_to_event_target(FlyString const&) override { return *this; }

private:
    MathMLElement(DOM::Document&, DOM::QualifiedName);

    virtual void visit_edges(Visitor&) override;

    virtual void initialize(JS::Realm&) override;

    JS::GCPtr<HTML::DOMStringMap> m_dataset;
};

}
