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

    virtual void config_string_did_change(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, DeprecatedString const& value);
    virtual void config_i32_did_change(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, i32 value);
    virtual void config_u32_did_change(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, u32 value);
    virtual void config_bool_did_change(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key, bool value);
    virtual void config_key_was_removed(DeprecatedString const& domain, DeprecatedString const& group, DeprecatedString const& key);
    virtual void config_group_was_removed(DeprecatedString const& domain, DeprecatedString const& group);
    virtual void config_group_was_added(DeprecatedString const& domain, DeprecatedString const& group);

protected:
    Listener();
};

}
