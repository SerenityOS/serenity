/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDataListElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDataListElement, HTMLElement);

public:
    virtual ~HTMLDataListElement() override;

    virtual DeprecatedFlyString default_role() const override { return DOM::ARIARoleNames::listbox; }

private:
    HTMLDataListElement(DOM::Document&, DOM::QualifiedName);
};

}
