/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/HTML/NavigatorConcurrentHardware.h>
#include <LibWeb/HTML/NavigatorID.h>
#include <LibWeb/HTML/NavigatorLanguage.h>
#include <LibWeb/HTML/NavigatorOnLine.h>

namespace Web::HTML {

class Navigator : public Bindings::PlatformObject
    , public NavigatorConcurrentHardwareMixin
    , public NavigatorIDMixin
    , public NavigatorLanguageMixin
    , public NavigatorOnLineMixin {
    WEB_PLATFORM_OBJECT(Navigator, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<Navigator> create(JS::Realm&);

    // FIXME: Implement NavigatorContentUtilsMixin

    // NavigatorCookies
    // FIXME: Hook up to Agent level state
    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-cookieenabled
    bool cookie_enabled() const { return true; }

    // NavigatorPlugins
    // FIXME: Actually support pdf viewing
    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-javaenabled
    bool java_enabled() const { return false; }

    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-pdfviewerenabled
    bool pdf_viewer_enabled() const { return false; }

    virtual ~Navigator() override;

private:
    explicit Navigator(JS::Realm&);
};

}
