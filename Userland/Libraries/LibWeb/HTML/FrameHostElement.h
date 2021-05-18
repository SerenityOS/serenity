/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class FrameHostElement : public HTMLElement {
public:
    FrameHostElement(DOM::Document&, QualifiedName);
    virtual ~FrameHostElement() override;

    Frame* content_frame() { return m_content_frame; }
    const Frame* content_frame() const { return m_content_frame; }

    const DOM::Document* content_document() const;

    Origin content_origin() const;
    bool may_access_from_origin(const Origin&) const;

    void content_frame_did_load(Badge<Fetch::FrameLoader>);

    virtual void inserted() override;

protected:
    RefPtr<Frame> m_content_frame;
};

}
