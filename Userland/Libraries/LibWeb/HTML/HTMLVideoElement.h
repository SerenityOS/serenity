/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLMediaElement.h>

namespace Web::HTML {

class HTMLVideoElement final : public HTMLMediaElement {
    WEB_PLATFORM_OBJECT(HTMLVideoElement, HTMLMediaElement);

public:
    virtual ~HTMLVideoElement() override;

    void set_video_width(u32 video_width) { m_video_width = video_width; }
    u32 video_width() const;

    void set_video_height(u32 video_height) { m_video_height = video_height; }
    u32 video_height() const;

private:
    HTMLVideoElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

    u32 m_video_width { 0 };
    u32 m_video_height { 0 };
};

}
