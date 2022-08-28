/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLMediaElement.h>

namespace Web::HTML {

class HTMLAudioElement final : public HTMLMediaElement {
    WEB_PLATFORM_OBJECT(HTMLAudioElement, HTMLMediaElement);

public:
    virtual ~HTMLAudioElement() override;

private:
    HTMLAudioElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(HTMLAudioElement, Web::HTML)
