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

    virtual void config_string_did_change(StringView domain, StringView group, StringView key, StringView value);
    virtual void config_i32_did_change(StringView domain, StringView group, StringView key, i32 value);
    virtual void config_u32_did_change(StringView domain, StringView group, StringView key, u32 value);
    virtual void config_bool_did_change(StringView domain, StringView group, StringView key, bool value);
    virtual void config_key_was_removed(StringView domain, StringView group, StringView key);
    virtual void config_group_was_removed(StringView domain, StringView group);
    virtual void config_group_was_added(StringView domain, StringView group);

protected:
    Listener();
};

}
