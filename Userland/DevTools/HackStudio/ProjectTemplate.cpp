/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectTemplate.h"
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <fcntl.h>
#include <spawn.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

namespace HackStudio {

ProjectTemplate::ProjectTemplate(const String& id, const String& name, const String& description, const GUI::Icon& icon, int priority)
    : m_id(id)
    , m_name(name)
    , m_description(description)
    , m_icon(icon)
    , m_priority(priority)
{
}

RefPtr<ProjectTemplate> ProjectTemplate::load_from_manifest(const String& manifest_path)
{
    auto config = Core::ConfigFile::open(manifest_path);

    if (!config->has_group("HackStudioTemplate")
        || !config->has_key("HackStudioTemplate", "Name")
        || !config->has_key("HackStudioTemplate", "Description")
        || !config->has_key("HackStudioTemplate", "IconName32x"))
        return {};

    auto id = LexicalPath::title(manifest_path);
    auto name = config->read_entry("HackStudioTemplate", "Name");
    auto description = config->read_entry("HackStudioTemplate", "Description");
    int priority = config->read_num_entry("HackStudioTemplate", "Priority", 0);

    // Attempt to read in the template icons
    // Fallback to a generic executable icon if one isn't found
    auto icon = GUI::Icon::default_icon("filetype-executable");

    auto bitmap_path_32 = String::formatted("/res/icons/hackstudio/templates-32x32/{}.png", config->read_entry("HackStudioTemplate", "IconName32x"));

    if (Core::File::exists(bitmap_path_32)) {
        auto bitmap_or_error = Gfx::Bitmap::try_load_from_file(bitmap_path_32);
        if (!bitmap_or_error.is_error())
            icon = GUI::Icon(bitmap_or_error.release_value());
    }

    return adopt_ref(*new ProjectTemplate(id, name, description, icon, priority));
}

Result<void, String> ProjectTemplate::create_project(const String& name, const String& path)
{
    // Check if a file or directory already exists at the project path
    if (Core::File::exists(path))
        return String("File or directory already exists at specified location.");

    dbgln("Creating project at path '{}' with name '{}'", path, name);

    // Verify that the template content directory exists. If it does, copy it's contents.
    // Otherwise, create an empty directory at the project path.
    if (Core::File::is_directory(content_path())) {
        auto result = Core::File::copy_file_or_directory(path, content_path());
        dbgln("Copying {} -> {}", content_path(), path);
        if (result.is_error())
            return String::formatted("Failed to copy template contents. Error code: {}", static_cast<Error const&>(result.error()));
    } else {
        dbgln("No template content directory found for '{}', creating an empty directory for the project.", m_id);
        int rc;
        if ((rc = mkdir(path.characters(), 0755)) < 0) {
            return String::formatted("Failed to mkdir empty project directory, error: {}, rc: {}.", strerror(errno), rc);
        }
    }

    // Check for an executable post-create script in $TEMPLATES_DIR/$ID.postcreate,
    // and run it with the path and name

    auto postcreate_script_path = LexicalPath::canonicalized_path(String::formatted("{}/{}.postcreate", templates_path(), m_id));
    struct stat postcreate_st;
    int result = stat(postcreate_script_path.characters(), &postcreate_st);
    if (result == 0 && (postcreate_st.st_mode & S_IXOTH) == S_IXOTH) {
        dbgln("Running post-create script '{}'", postcreate_script_path);

        // Generate a namespace-safe project name (replace hyphens with underscores)
        auto namespace_safe = name.replace("-", "_", true);

        pid_t child_pid;
        const char* argv[] = { postcreate_script_path.characters(), name.characters(), path.characters(), namespace_safe.characters(), nullptr };

        if ((errno = posix_spawn(&child_pid, postcreate_script_path.characters(), nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
            return String("Failed to spawn project post-create script.");
        }

        // Command spawned, wait for exit.
        int status;
        if (waitpid(child_pid, &status, 0) < 0)
            return String("Failed to spawn project post-create script.");

        int child_error = WEXITSTATUS(status);
        dbgln("Post-create script exited with code {}", child_error);

        if (child_error != 0)
            return String("Project post-creation script exited with non-zero error code.");
    }

    return {};
}

}
