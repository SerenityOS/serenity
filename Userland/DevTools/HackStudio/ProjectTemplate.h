/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/DeprecatedString.h>
#include <AK/LexicalPath.h>
#include <AK/RefCounted.h>
#include <AK/Result.h>
#include <AK/Weakable.h>
#include <LibGUI/Icon.h>

namespace HackStudio {

class ProjectTemplate : public RefCounted<ProjectTemplate> {
public:
    static DeprecatedString templates_path() { return "/res/devel/templates"; }

    static RefPtr<ProjectTemplate> load_from_manifest(DeprecatedString const& manifest_path);

    explicit ProjectTemplate(DeprecatedString const& id, DeprecatedString const& name, DeprecatedString const& description, const GUI::Icon& icon, int priority);

    Result<void, DeprecatedString> create_project(DeprecatedString const& name, DeprecatedString const& path);

    DeprecatedString const& id() const { return m_id; }
    DeprecatedString const& name() const { return m_name; }
    DeprecatedString const& description() const { return m_description; }
    const GUI::Icon& icon() const { return m_icon; }
    const DeprecatedString content_path() const
    {
        return LexicalPath::canonicalized_path(DeprecatedString::formatted("{}/{}", templates_path(), m_id));
    }
    int priority() const { return m_priority; }

private:
    DeprecatedString m_id;
    DeprecatedString m_name;
    DeprecatedString m_description;
    GUI::Icon m_icon;
    int m_priority { 0 };
};

}
