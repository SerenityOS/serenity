/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class BrowsingContextContainer : public HTMLElement {
public:
    BrowsingContextContainer(DOM::Document&, QualifiedName);
    virtual ~BrowsingContextContainer() override;

    BrowsingContext* nested_browsing_context() { return m_nested_browsing_context; }
    const BrowsingContext* nested_browsing_context() const { return m_nested_browsing_context; }

    const DOM::Document* content_document() const;

    Origin content_origin() const;
    bool may_access_from_origin(const Origin&) const;

    void nested_browsing_context_did_load(Badge<FrameLoader>);

    virtual void inserted() override;

protected:
    RefPtr<BrowsingContext> m_nested_browsing_context;
};

}
