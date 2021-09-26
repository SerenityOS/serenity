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

    HTMLIFrameElement(DOM::Document&, QualifiedName);
    virtual ~HTMLIFrameElement() override;

    virtual RefPtr<Layout::Node> create_layout_node() override;

private:
    virtual void inserted() override;
    virtual void parse_attribute(const FlyString& name, const String& value) override;

    void load_src(const String&);
};

void run_iframe_load_event_steps(HTML::HTMLIFrameElement&);

}
