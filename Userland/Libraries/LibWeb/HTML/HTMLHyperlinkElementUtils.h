/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/EventLoop/Task.h>
#include <LibWeb/HTML/TokenizedFeatures.h>

namespace Web::HTML {

class HTMLHyperlinkElementUtils {
public:
    virtual ~HTMLHyperlinkElementUtils();

    DeprecatedString origin() const;

    DeprecatedString href() const;
    WebIDL::ExceptionOr<void> set_href(StringView);

    DeprecatedString protocol() const;
    void set_protocol(StringView);

    DeprecatedString username() const;
    void set_username(StringView);

    DeprecatedString password() const;
    void set_password(StringView);

    DeprecatedString host() const;
    void set_host(StringView);

    DeprecatedString hostname() const;
    void set_hostname(StringView);

    DeprecatedString port() const;
    void set_port(StringView);

    DeprecatedString pathname() const;
    void set_pathname(StringView);

    DeprecatedString search() const;
    void set_search(StringView);

    DeprecatedString hash() const;
    void set_hash(StringView);

protected:
    virtual DOM::Document& hyperlink_element_utils_document() = 0;
    virtual DeprecatedString hyperlink_element_utils_href() const = 0;
    virtual WebIDL::ExceptionOr<void> set_hyperlink_element_utils_href(DeprecatedString) = 0;
    virtual bool hyperlink_element_utils_is_html_anchor_element() const = 0;
    virtual bool hyperlink_element_utils_is_connected() const = 0;
    virtual DeprecatedString hyperlink_element_utils_get_an_elements_target() const = 0;
    virtual TokenizedFeature::NoOpener hyperlink_element_utils_get_an_elements_noopener(StringView target) const = 0;

    virtual void hyperlink_element_utils_queue_an_element_task(HTML::Task::Source source, Function<void()> steps) = 0;

    void set_the_url();
    void follow_the_hyperlink(Optional<DeprecatedString> hyperlink_suffix);

private:
    void reinitialize_url() const;
    void update_href();
    bool cannot_navigate() const;

    Optional<AK::URL> m_url;
};

}
