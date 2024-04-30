/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <LibURL/Forward.h>

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
        ByteString name;
        ByteString executable;
        Vector<ByteString> arguments;
        LauncherType launcher_type { LauncherType::Default };

        static NonnullRefPtr<Details> from_details_str(ByteString const&);
    };

    static void ensure_connection();
    static ErrorOr<void> add_allowed_url(URL::URL const&);
    static ErrorOr<void> add_allowed_handler_with_any_url(ByteString const& handler);
    static ErrorOr<void> add_allowed_handler_with_only_specific_urls(ByteString const& handler, Vector<URL::URL> const&);
    static ErrorOr<void> seal_allowlist();
    static bool open(const URL::URL&, ByteString const& handler_name = {});
    static bool open(const URL::URL&, Details const& details);
    static Vector<ByteString> get_handlers_for_url(const URL::URL&);
    static Vector<NonnullRefPtr<Details>> get_handlers_with_details_for_url(const URL::URL&);
};

}
