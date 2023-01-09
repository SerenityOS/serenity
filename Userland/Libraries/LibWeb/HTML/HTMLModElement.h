/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLModElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLModElement, HTMLElement);

public:
    virtual ~HTMLModElement() override;

    virtual DeprecatedFlyString default_role() const override;

private:
    HTMLModElement(DOM::Document&, DOM::QualifiedName);
};

}
