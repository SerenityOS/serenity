/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLHRElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLHRElement, HTMLElement);

public:
    virtual ~HTMLHRElement() override;

    // https://www.w3.org/TR/html-aria/#el-hr
    virtual Optional<DOM::ARIARoles::Role> default_role() const override { return DOM::ARIARoles::Role::separator; }

private:
    HTMLHRElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
