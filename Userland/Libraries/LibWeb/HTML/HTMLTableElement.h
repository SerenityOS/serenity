/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTableElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTableElementWrapper;

    HTMLTableElement(DOM::Document&, QualifiedName);
    virtual ~HTMLTableElement() override;

private:
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;
};

}
