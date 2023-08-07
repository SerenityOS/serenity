/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTitleElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLTitleElement, HTMLElement);

public:
    virtual ~HTMLTitleElement() override;

private:
    HTMLTitleElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
    virtual void children_changed() override;
};

}
