/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/HTML/MimeTypeArray.h>
#include <LibWeb/HTML/NavigatorConcurrentHardware.h>
#include <LibWeb/HTML/NavigatorID.h>
#include <LibWeb/HTML/NavigatorLanguage.h>
#include <LibWeb/HTML/NavigatorOnLine.h>
#include <LibWeb/HTML/PluginArray.h>

namespace Web::HTML {

class Navigator : public Bindings::PlatformObject
    , public NavigatorConcurrentHardwareMixin
    , public NavigatorIDMixin
    , public NavigatorLanguageMixin
    , public NavigatorOnLineMixin {
    WEB_PLATFORM_OBJECT(Navigator, Bindings::PlatformObject);

public:
    [[nodiscard]] static JS::NonnullGCPtr<Navigator> create(JS::Realm&);

    // FIXME: Implement NavigatorContentUtilsMixin

    // NavigatorCookies
    // FIXME: Hook up to Agent level state
    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-cookieenabled
    bool cookie_enabled() const { return true; }

    // NavigatorPlugins
    // https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-javaenabled
    bool java_enabled() const { return false; }

    bool pdf_viewer_enabled() const;

    bool webdriver() const;

    [[nodiscard]] JS::NonnullGCPtr<MimeTypeArray> mime_types();
    [[nodiscard]] JS::NonnullGCPtr<PluginArray> plugins();

    virtual ~Navigator() override;

protected:
    virtual void visit_edges(Cell::Visitor&) override;

private:
    explicit Navigator(JS::Realm&);

    virtual void initialize(JS::Realm&) override;

    JS::GCPtr<PluginArray> m_plugin_array;
    JS::GCPtr<MimeTypeArray> m_mime_type_array;
};

}
