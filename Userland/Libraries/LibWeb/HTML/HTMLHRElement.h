/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLHRElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLHRElement, HTMLElement);

public:
    virtual ~HTMLHRElement() override;

private:
    HTMLHRElement(DOM::Document&, DOM::QualifiedName);
};

}
