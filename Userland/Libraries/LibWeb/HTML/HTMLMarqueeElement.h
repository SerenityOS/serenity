/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

// NOTE: This element is marked as obsolete, but is still listed as required by the specification.
class HTMLMarqueeElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLMarqueeElement, HTMLElement);

public:
    virtual ~HTMLMarqueeElement() override;

private:
    HTMLMarqueeElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}
