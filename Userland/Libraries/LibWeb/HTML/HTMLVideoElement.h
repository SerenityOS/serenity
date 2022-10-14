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

private:
    HTMLVideoElement(DOM::Document&, DOM::QualifiedName);
};

}
