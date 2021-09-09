/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Scripting/ClassicScript.h>

namespace Web::HTML {

NonnullRefPtr<ClassicScript> ClassicScript::create(URL base_url, RefPtr<JS::Script> script_record)
{
    return adopt_ref(*new ClassicScript(move(base_url), move(script_record)));
}

ClassicScript::ClassicScript(URL base_url, RefPtr<JS::Script> script_record)
    : Script(move(base_url))
    , m_script_record(move(script_record))
{
}

ClassicScript::~ClassicScript()
{
}

}
