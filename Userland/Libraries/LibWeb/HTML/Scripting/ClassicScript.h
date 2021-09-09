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
    static NonnullRefPtr<ClassicScript> create(URL base_url, RefPtr<JS::Script>);

    JS::Script* script_record() { return m_script_record; }
    JS::Script const* script_record() const { return m_script_record; }

private:
    explicit ClassicScript(URL base_url, RefPtr<JS::Script>);

    RefPtr<JS::Script> m_script_record;
};

}
