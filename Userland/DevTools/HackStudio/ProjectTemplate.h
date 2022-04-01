/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    static RefPtr<ProjectTemplate> load_from_manifest(String const& manifest_path);

    explicit ProjectTemplate(String const& id, String const& name, String const& description, const GUI::Icon& icon, int priority);

    Result<void, String> create_project(String const& name, String const& path);

    String const& id() const { return m_id; }
    String const& name() const { return m_name; }
    String const& description() const { return m_description; }
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
