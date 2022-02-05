/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLBRElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLBRElementWrapper;

    HTMLBRElement(DOM::Document&, QualifiedName);
    virtual ~HTMLBRElement() override;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;
};

}
