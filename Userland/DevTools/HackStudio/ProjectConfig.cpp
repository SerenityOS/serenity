/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectConfig.h"
#include <AK/NonnullOwnPtr.h>
#include <LibCore/File.h>

namespace HackStudio {

ProjectConfig::ProjectConfig(JsonObject config)
    : m_config(move(config))
{
}

ErrorOr<NonnullOwnPtr<ProjectConfig>> ProjectConfig::try_load_project_config(ByteString path)
{
    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
    auto file_contents = TRY(file->read_until_eof());

    auto json = TRY(JsonValue::from_string(file_contents));
    if (!json.is_object())
        return Error::from_string_literal("The topmost JSON element is not an object");

    return try_make<ProjectConfig>(json.as_object());
}

NonnullOwnPtr<ProjectConfig> ProjectConfig::create_empty()
{
    JsonObject empty {};
    return adopt_own(*new ProjectConfig(empty));
}

Optional<ByteString> ProjectConfig::read_key(ByteString key_name) const
{
    return m_config.get_byte_string(key_name);
}

}
