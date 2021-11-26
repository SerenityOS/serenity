/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/Forward.h>

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
    virtual DOM::Document const& hyperlink_element_utils_document() const = 0;
    virtual String hyperlink_element_utils_href() const = 0;
    virtual void set_hyperlink_element_utils_href(String) = 0;

    void set_the_url();

private:
    void reinitialize_url() const;
    void update_href();

    Optional<AK::URL> m_url;
};

}
