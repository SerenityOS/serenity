/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectConfig.h"
#include <AK/NonnullOwnPtr.h>
#include <LibCore/Stream.h>

namespace HackStudio {

ProjectConfig::ProjectConfig(JsonObject config)
    : m_config(move(config))
{
}

ErrorOr<NonnullOwnPtr<ProjectConfig>> ProjectConfig::try_load_project_config(DeprecatedString path)
{
    auto file = TRY(Core::Stream::File::open(path, Core::Stream::OpenMode::Read));
    auto file_contents = TRY(file->read_until_eof());

    auto json = TRY(JsonValue::from_string(file_contents));
    if (!json.is_object())
        return Error::from_string_literal("The topmost JSON element is not an object");

    return adopt_own(*new ProjectConfig(json.as_object()));
}

NonnullOwnPtr<ProjectConfig> ProjectConfig::create_empty()
{
    JsonObject empty {};
    return adopt_own(*new ProjectConfig(empty));
}

Optional<DeprecatedString> ProjectConfig::read_key(DeprecatedString key_name) const
{
    auto const& value = m_config.get(key_name);
    if (!value.is_string())
        return {};

    return { value.as_string() };
}

}
