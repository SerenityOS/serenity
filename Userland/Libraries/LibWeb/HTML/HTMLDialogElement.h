/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDialogElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDialogElement, HTMLElement);

public:
    virtual ~HTMLDialogElement() override;

    // https://www.w3.org/TR/html-aria/#el-dialog
    virtual DeprecatedFlyString default_role() const override { return DOM::ARIARoleNames::dialog; }

private:
    HTMLDialogElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
