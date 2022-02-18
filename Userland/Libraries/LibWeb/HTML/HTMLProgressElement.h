/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLProgressElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLProgressElementWrapper;

    HTMLProgressElement(DOM::Document&, QualifiedName);
    virtual ~HTMLProgressElement() override;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    double value() const;
    void set_value(double);

    double max() const;
    void set_max(double value);

    double position() const;

private:
    bool is_determinate() const { return has_attribute(HTML::AttributeNames::value); }
};

}
