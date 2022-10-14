/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>

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
        DeprecatedString name;
        DeprecatedString executable;
        LauncherType launcher_type { LauncherType::Default };

        static NonnullRefPtr<Details> from_details_str(DeprecatedString const&);
    };

    static void ensure_connection();
    static ErrorOr<void> add_allowed_url(URL const&);
    static ErrorOr<void> add_allowed_handler_with_any_url(DeprecatedString const& handler);
    static ErrorOr<void> add_allowed_handler_with_only_specific_urls(DeprecatedString const& handler, Vector<URL> const&);
    static ErrorOr<void> seal_allowlist();
    static bool open(const URL&, DeprecatedString const& handler_name = {});
    static bool open(const URL&, Details const& details);
    static Vector<DeprecatedString> get_handlers_for_url(const URL&);
    static NonnullRefPtrVector<Details> get_handlers_with_details_for_url(const URL&);
};

}
