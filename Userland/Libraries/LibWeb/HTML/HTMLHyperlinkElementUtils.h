/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/EventLoop/Task.h>

namespace Web::HTML {

class HTMLHyperlinkElementUtils {
public:
    virtual ~HTMLHyperlinkElementUtils();

    String origin() const;

    String href() const;
    void set_href(String);

    String protocol() const;
    void set_protocol(String);

    String username() const;
    void set_username(String);

    String password() const;
    void set_password(String);

    String host() const;
    void set_host(String);

    String hostname() const;
    void set_hostname(String);

    String port() const;
    void set_port(String);

    String pathname() const;
    void set_pathname(String);

    String search() const;
    void set_search(String);

    String hash() const;
    void set_hash(String);

protected:
    virtual DOM::Document& hyperlink_element_utils_document() = 0;
    virtual String hyperlink_element_utils_href() const = 0;
    virtual void set_hyperlink_element_utils_href(String) = 0;
    virtual bool hyperlink_element_utils_is_html_anchor_element() const = 0;
    virtual bool hyperlink_element_utils_is_connected() const = 0;
    virtual String hyperlink_element_utils_target() const = 0;
    virtual void hyperlink_element_utils_queue_an_element_task(HTML::Task::Source source, Function<void()> steps) = 0;

    void set_the_url();
    void follow_the_hyperlink(Optional<String> hyperlink_suffix);

private:
    void reinitialize_url() const;
    void update_href();
    bool cannot_navigate() const;
    String get_an_elements_target() const;
    bool get_an_elements_noopener(StringView target) const;

    Optional<AK::URL> m_url;
};

}
