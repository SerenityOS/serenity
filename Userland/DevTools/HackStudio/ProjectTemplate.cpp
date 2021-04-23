/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectTemplate.h"
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <assert.h>
#include <fcntl.h>
#include <spawn.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

// FIXME: shameless copy+paste from Userland/cp. We should have system-wide file management functions.
// Issue #5209
bool copy_file_or_directory(String, String, bool, bool);
bool copy_file(String, String, const struct stat&, int);
bool copy_directory(String, String, bool);

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

    auto id = LexicalPath(manifest_path).title();
    auto name = config->read_entry("HackStudioTemplate", "Name");
    auto description = config->read_entry("HackStudioTemplate", "Description");
    int priority = config->read_num_entry("HackStudioTemplate", "Priority", 0);

    // Attempt to read in the template icons
    // Fallback to a generic executable icon if one isn't found
    auto icon = GUI::Icon::default_icon("filetype-executable");

    auto bitmap_path_32 = String::formatted("/res/icons/hackstudio/templates-32x32/{}.png", config->read_entry("HackStudioTemplate", "IconName32x"));

    if (Core::File::exists(bitmap_path_32)) {
        auto bitmap32 = Gfx::Bitmap::load_from_file(bitmap_path_32);
        icon = GUI::Icon(move(bitmap32));
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
        if (!copy_directory(content_path(), path, false))
            return String("Failed to copy template contents.");
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
        String namespace_safe(name.characters());
        namespace_safe.replace("-", "_", true);

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

// FIXME: shameless copy+paste from Userland/cp. We should have system-wide file management functions.
// Issue #5209
bool copy_file_or_directory(String src_path, String dst_path, bool recursion_allowed, bool link)
{
    int src_fd = open(src_path.characters(), O_RDONLY);
    if (src_fd < 0) {
        perror("open src");
        return false;
    }

    struct stat src_stat;
    int rc = fstat(src_fd, &src_stat);
    if (rc < 0) {
        perror("stat src");
        return false;
    }

    if (S_ISDIR(src_stat.st_mode)) {
        if (!recursion_allowed) {
            fprintf(stderr, "cp: -R not specified; omitting directory '%s'\n", src_path.characters());
            return false;
        }
        return copy_directory(src_path, dst_path, link);
    }
    if (link) {
        if (::link(src_path.characters(), dst_path.characters()) < 0) {
            perror("link");
            return false;
        }
        return true;
    }

    return copy_file(src_path, dst_path, src_stat, src_fd);
}

bool copy_file(String src_path, String dst_path, const struct stat& src_stat, int src_fd)
{
    // Get umask
    auto my_umask = umask(0);
    umask(my_umask);

    // NOTE: We don't copy the set-uid and set-gid bits.
    mode_t mode = (src_stat.st_mode & ~my_umask) & ~06000;

    int dst_fd = creat(dst_path.characters(), mode);
    if (dst_fd < 0) {
        if (errno != EISDIR) {
            perror("open dst");
            return false;
        }
        StringBuilder builder;
        builder.append(dst_path);
        builder.append('/');
        builder.append(LexicalPath(src_path).basename());
        dst_path = builder.to_string();
        dst_fd = creat(dst_path.characters(), 0666);
        if (dst_fd < 0) {
            perror("open dst");
            return false;
        }
    }

    if (src_stat.st_size > 0) {
        if (ftruncate(dst_fd, src_stat.st_size) < 0) {
            perror("cp: ftruncate");
            return false;
        }
    }

    for (;;) {
        char buffer[32768];
        ssize_t nread = read(src_fd, buffer, sizeof(buffer));
        if (nread < 0) {
            perror("read src");
            return false;
        }
        if (nread == 0)
            break;
        ssize_t remaining_to_write = nread;
        char* bufptr = buffer;
        while (remaining_to_write) {
            ssize_t nwritten = write(dst_fd, bufptr, remaining_to_write);
            if (nwritten < 0) {
                perror("write dst");
                return false;
            }
            assert(nwritten > 0);
            remaining_to_write -= nwritten;
            bufptr += nwritten;
        }
    }

    close(src_fd);
    close(dst_fd);
    return true;
}

bool copy_directory(String src_path, String dst_path, bool link)
{
    int rc = mkdir(dst_path.characters(), 0755);
    if (rc < 0) {
        perror("cp: mkdir");
        return false;
    }

    String src_rp = Core::File::real_path_for(src_path);
    src_rp = String::formatted("{}/", src_rp);
    String dst_rp = Core::File::real_path_for(dst_path);
    dst_rp = String::formatted("{}/", dst_rp);

    if (!dst_rp.is_empty() && dst_rp.starts_with(src_rp)) {
        fprintf(stderr, "cp: Cannot copy %s into itself (%s)\n",
            src_path.characters(), dst_path.characters());
        return false;
    }

    Core::DirIterator di(src_path, Core::DirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "cp: DirIterator: %s\n", di.error_string());
        return false;
    }
    while (di.has_next()) {
        String filename = di.next_path();
        bool is_copied = copy_file_or_directory(
            String::formatted("{}/{}", src_path, filename),
            String::formatted("{}/{}", dst_path, filename),
            true, link);
        if (!is_copied) {
            return false;
        }
    }
    return true;
}
