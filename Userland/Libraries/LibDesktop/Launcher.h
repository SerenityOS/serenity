/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/String.h>

namespace Desktop {

class Launcher {
public:
    enum class LauncherType {
        Default = 0,
        Application,
        UserPreferred,
        UserDefault
    };

    struct Details : public RefCounted<Details> {
        String name;
        String executable;
        LauncherType launcher_type { LauncherType::Default };

        static NonnullRefPtr<Details> from_details_str(String const&);
    };

    [[nodiscard]] static bool add_allowed_url(URL const&);
    [[nodiscard]] static bool add_allowed_handler_with_any_url(String const& handler);
    [[nodiscard]] static bool add_allowed_handler_with_only_specific_urls(String const& handler, const Vector<URL>&);
    [[nodiscard]] static bool seal_allowlist();
    static bool open(URL const&, String const& handler_name = {});
    static bool open(URL const&, Details const& details);
    static Vector<String> get_handlers_for_url(URL const&);
    static NonnullRefPtrVector<Details> get_handlers_with_details_for_url(URL const&);
};

}
