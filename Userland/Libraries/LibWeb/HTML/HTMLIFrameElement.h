/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/BrowsingContextContainer.h>

namespace Web::HTML {

class HTMLIFrameElement final : public BrowsingContextContainer {
public:
    using WrapperType = Bindings::HTMLIFrameElementWrapper;

    HTMLIFrameElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLIFrameElement() override;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

private:
    virtual void inserted() override;
    virtual void removed_from(Node*) override;
    virtual void parse_attribute(FlyString const& name, String const& value) override;

    void load_src(String const&);
};

void run_iframe_load_event_steps(HTML::HTMLIFrameElement&);

}
