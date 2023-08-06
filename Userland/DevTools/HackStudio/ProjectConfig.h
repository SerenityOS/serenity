/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/JsonObject.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <LibCore/EventReceiver.h>

namespace HackStudio {

class ProjectConfig {
public:
    static ErrorOr<NonnullOwnPtr<ProjectConfig>> try_load_project_config(DeprecatedString path);
    static NonnullOwnPtr<ProjectConfig> create_empty();

    ProjectConfig(JsonObject);

    Optional<DeprecatedString> build_command() const { return read_key("build_command"); }
    Optional<DeprecatedString> run_command() const { return read_key("run_command"); }

private:
    Optional<DeprecatedString> read_key(DeprecatedString key_name) const;

    JsonObject m_config;
};

}
