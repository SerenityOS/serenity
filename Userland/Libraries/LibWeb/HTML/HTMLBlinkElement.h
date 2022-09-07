/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Forward.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLBlinkElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLBlinkElement, HTMLElement);

public:
    virtual ~HTMLBlinkElement() override;

private:
    HTMLBlinkElement(DOM::Document&, DOM::QualifiedName);

    void blink();

    NonnullRefPtr<Platform::Timer> m_timer;
};

}

WRAPPER_HACK(HTMLBlinkElement, Web::HTML)
