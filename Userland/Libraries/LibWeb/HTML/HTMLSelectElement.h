/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLSelectElement final : public FormAssociatedElement {
public:
    using WrapperType = Bindings::HTMLSelectElementWrapper;

    HTMLSelectElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLSelectElement() override;

private:
    // ^DOM::Node
    virtual void inserted() override;
    virtual void removed_from(DOM::Node*) override;
};

}
