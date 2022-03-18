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
#include <LibCore/Object.h>

namespace HackStudio {

class ProjectConfig {
public:
    static ErrorOr<NonnullOwnPtr<ProjectConfig>> try_load_project_config(String path);
    static NonnullOwnPtr<ProjectConfig> create_empty();

    ProjectConfig(JsonObject);

    Optional<String> build_command() const { return read_key("build_command"); }
    Optional<String> run_command() const { return read_key("run_command"); }

private:
    Optional<String> read_key(String key_name) const;

    JsonObject m_config;
};

}
