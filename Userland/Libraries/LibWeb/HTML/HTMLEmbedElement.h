/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLEmbedElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLEmbedElement, HTMLElement);

public:
    virtual ~HTMLEmbedElement() override;

private:
    HTMLEmbedElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
