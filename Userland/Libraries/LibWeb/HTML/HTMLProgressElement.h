/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/ARIARoleNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLProgressElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLProgressElement, HTMLElement);

public:
    virtual ~HTMLProgressElement() override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    double value() const;
    void set_value(double);

    double max() const;
    void set_max(double value);

    double position() const;

    // ^HTMLElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const override { return true; }

    bool using_system_appearance() const;

    // https://www.w3.org/TR/html-aria/#el-progress
    virtual DeprecatedFlyString default_role() const override { return DOM::ARIARoleNames::progressbar; }

private:
    HTMLProgressElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    void progress_position_updated();

    bool is_determinate() const { return has_attribute(HTML::AttributeNames::value); }
};

}
