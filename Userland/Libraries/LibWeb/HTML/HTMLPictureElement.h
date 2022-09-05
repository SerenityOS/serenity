/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLPictureElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLPictureElement, HTMLElement);

public:
    virtual ~HTMLPictureElement() override;

private:
    HTMLPictureElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(HTMLPictureElement, Web::HTML)
