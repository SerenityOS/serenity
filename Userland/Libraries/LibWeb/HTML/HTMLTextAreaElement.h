/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTextAreaElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLTextAreaElementWrapper;

    HTMLTextAreaElement(DOM::Document&, QualifiedName);
    virtual ~HTMLTextAreaElement() override;

    String const& type() const
    {
        static String textarea = "textarea";
        return textarea;
    }
};

}
