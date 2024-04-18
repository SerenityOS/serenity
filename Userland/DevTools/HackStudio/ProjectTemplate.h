/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/ByteString.h>
#include <AK/LexicalPath.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibGUI/Icon.h>

namespace HackStudio {

class ProjectTemplate : public RefCounted<ProjectTemplate> {
public:
    static ByteString templates_path() { return "/res/devel/templates"; }

    static RefPtr<ProjectTemplate> load_from_manifest(ByteString const& manifest_path);

    explicit ProjectTemplate(ByteString const& id, ByteString const& name, ByteString const& description, const GUI::Icon& icon, int priority);

    ErrorOr<void> create_project(ByteString const& name, ByteString const& path);

    ByteString const& id() const { return m_id; }
    ByteString const& name() const { return m_name; }
    ByteString const& description() const { return m_description; }
    const GUI::Icon& icon() const { return m_icon; }
    ByteString const content_path() const
    {
        return LexicalPath::canonicalized_path(ByteString::formatted("{}/{}", templates_path(), m_id));
    }
    int priority() const { return m_priority; }

private:
    ByteString m_id;
    ByteString m_name;
    ByteString m_description;
    GUI::Icon m_icon;
    int m_priority { 0 };
};

}
