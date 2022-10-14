/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLMetaElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLMetaElement, HTMLElement);

public:
    virtual ~HTMLMetaElement() override;

private:
    HTMLMetaElement(DOM::Document&, DOM::QualifiedName);
};

}
