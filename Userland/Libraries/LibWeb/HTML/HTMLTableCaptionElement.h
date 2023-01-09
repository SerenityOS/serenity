/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTableCaptionElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLTableCaptionElement, HTMLElement);

public:
    virtual ~HTMLTableCaptionElement() override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    // https://www.w3.org/TR/html-aria/#el-caption
    virtual DeprecatedFlyString default_role() const override { return DOM::ARIARoleNames::caption; }

private:
    HTMLTableCaptionElement(DOM::Document&, DOM::QualifiedName);
};

}
