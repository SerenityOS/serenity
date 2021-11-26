/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Script.h>
#include <LibWeb/HTML/Scripting/Script.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#classic-script
class ClassicScript final : public Script {
public:
    ~ClassicScript();

    enum class MutedErrors {
        No,
        Yes,
    };
    static NonnullRefPtr<ClassicScript> create(String filename, StringView source, JS::Realm&, AK::URL base_url, MutedErrors = MutedErrors::No);

    JS::Script* script_record() { return m_script_record; }
    JS::Script const* script_record() const { return m_script_record; }

    enum class RethrowErrors {
        No,
        Yes,
    };
    JS::Value run(RethrowErrors = RethrowErrors::No);

private:
    ClassicScript(AK::URL base_url, String filename);

    RefPtr<JS::Script> m_script_record;
    MutedErrors m_muted_errors { MutedErrors::No };
};

}
