/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLHeadElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLHeadElement, HTMLElement);

public:
    virtual ~HTMLHeadElement() override;

private:
    HTMLHeadElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
