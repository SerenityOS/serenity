/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLDataListElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDataListElement, HTMLElement);

public:
    virtual ~HTMLDataListElement() override;

    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::listbox; }

private:
    HTMLDataListElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
