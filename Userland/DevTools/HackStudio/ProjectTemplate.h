/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/LexicalPath.h>
#include <AK/RefCounted.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <AK/Weakable.h>
#include <LibGUI/Icon.h>

namespace HackStudio {

class ProjectTemplate : public RefCounted<ProjectTemplate> {
public:
    static String templates_path() { return "/res/devel/templates"; }

    static RefPtr<ProjectTemplate> load_from_manifest(const String& manifest_path);

    explicit ProjectTemplate(const String& id, const String& name, const String& description, const GUI::Icon& icon, int priority);

    Result<void, String> create_project(const String& name, const String& path);

    const String& id() const { return m_id; }
    const String& name() const { return m_name; }
    const String& description() const { return m_description; }
    const GUI::Icon& icon() const { return m_icon; }
    const String content_path() const
    {
        return LexicalPath::canonicalized_path(String::formatted("{}/{}", templates_path(), m_id));
    }
    int priority() const { return m_priority; }

private:
    String m_id;
    String m_name;
    String m_description;
    GUI::Icon m_icon;
    int m_priority { 0 };
};

}
