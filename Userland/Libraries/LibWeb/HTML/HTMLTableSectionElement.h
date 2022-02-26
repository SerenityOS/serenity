/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTableSectionElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTableSectionElementWrapper;

    HTMLTableSectionElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLTableSectionElement() override;

    NonnullRefPtr<DOM::HTMLCollection> rows() const;
};

}
