/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/ByteBuffer.h>
#include <YAK/LexicalPath.h>
#include <YAK/RefCounted.h>
#include <YAK/Result.h>
#include <YAK/String.h>
#include <YAK/Weakable.h>
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
