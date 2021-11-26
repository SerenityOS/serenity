/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace Config {

class Listener {
public:
    virtual ~Listener();

    static void for_each(Function<void(Listener&)>);

    virtual void config_string_did_change(String const& domain, String const& group, String const& key, String const& value);
    virtual void config_i32_did_change(String const& domain, String const& group, String const& key, i32 value);
    virtual void config_bool_did_change(String const& domain, String const& group, String const& key, bool value);
    virtual void config_key_was_removed(String const& domain, String const& group, String const& key);

protected:
    Listener();
};

}
