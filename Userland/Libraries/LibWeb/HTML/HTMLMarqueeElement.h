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
public:
    using WrapperType = Bindings::HTMLMarqueeElementWrapper;

    HTMLMarqueeElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLMarqueeElement() override;

private:
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}
