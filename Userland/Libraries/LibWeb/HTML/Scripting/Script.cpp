/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Scripting/Script.h>

namespace Web::HTML {

Script::Script(AK::URL base_url, String filename)
    : m_base_url(move(base_url))
    , m_filename(move(filename))
{
}

Script::~Script()
{
}

}
