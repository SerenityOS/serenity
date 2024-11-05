/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <sys/prctl.h>
#include <unistd.h>

#include "LayoutParsing.h"
#include "VFSRootContextLayout.h"

static ErrorOr<unsigned> create_vfs_root_context_unshare()
{
    return TRY(Core::System::unshare_create(Kernel::UnshareType::VFSRootContext, 0));
}

static ErrorOr<void> create_custom_vfs_root_context_layout(JsonArray const& layout_creation_sequence)
{
    auto vfs_root_context_index = TRY(create_vfs_root_context_unshare());
    char pattern[] = "/tmp/container_root_XXXXXX";
    auto temp_directory_path = TRY(Core::System::mkdtemp(pattern));
    auto vfs_root_context_layout = TRY(VFSRootContextLayout::create(temp_directory_path.bytes_as_string_view(), vfs_root_context_index));
    TRY(LayoutParsing::handle_creation_sequence(*vfs_root_context_layout, layout_creation_sequence));

    TRY(vfs_root_context_layout->apply_mounts_on_vfs_root_context_id());

    TRY(Core::System::rmdir(temp_directory_path));
    TRY(Core::System::unshare_attach(Kernel::UnshareType::VFSRootContext, vfs_root_context_index));

    // Finally, apply changes to current working directory.
    (void)chdir("/");
    return {};
}

static ErrorOr<void> create_and_attach_scoped_process_list_unshare()
{
    auto scoped_process_list_index = TRY(Core::System::unshare_create(Kernel::UnshareType::ScopedProcessList, 0));
    TRY(Core::System::unshare_attach(Kernel::UnshareType::ScopedProcessList, scoped_process_list_index));
    return {};
}

static ErrorOr<void> create_and_attach_hostname_context(StringView hostname)
{
    auto hostname_context_index = TRY(Core::System::unshare_create(Kernel::UnshareType::HostnameContext, 0));
    TRY(Core::System::unshare_attach(Kernel::UnshareType::HostnameContext, hostname_context_index));
    TRY(Core::System::sethostname(hostname));
    return {};
}

static ErrorOr<void> extract_values_from_file(Core::File& file, String& command, JsonArray& new_vfs_root_context_layout_creation_sequence, bool& pid_isolation, Optional<String>& hostname_context_name, bool& enforce_jail)
{
    auto file_contents = TRY(file.read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));
    auto configuration_object = json.as_object();
    if (!configuration_object.has_bool("jail"sv))
        return Error::from_string_literal("JSON configuration invalid: Jail enforcement flag is not specified");
    if (!configuration_object.has_bool("pid-isolation"sv))
        return Error::from_string_literal("JSON configuration invalid: PID isolation flag is not specified");
    if (!configuration_object.has_string("command"sv))
        return Error::from_string_literal("JSON configuration invalid: Command is not specified");
    if (!configuration_object.has_array("layout"sv))
        return Error::from_string_literal("JSON configuration invalid: VFS root context layout is not specified");

    if (configuration_object.has_null("hostname"sv) && configuration_object.has_string("hostname"sv))
        return Error::from_string_literal("JSON configuration invalid: Can't have hostname as null and string during configuration");
    if (!configuration_object.has_null("hostname"sv) && !configuration_object.has_string("hostname"sv))
        return Error::from_string_literal("JSON configuration invalid: Hostname is not specified");

    new_vfs_root_context_layout_creation_sequence = move(configuration_object.get("layout"sv).value().as_array());
    pid_isolation = configuration_object.get("pid-isolation"sv).value().as_bool();
    enforce_jail = configuration_object.get("jail"sv).value().as_bool();

    auto possible_hostname = configuration_object.get_byte_string("hostname"sv);
    if (possible_hostname.has_value())
        hostname_context_name = TRY(String::from_byte_string(possible_hostname.value()));

    command = TRY(String::from_byte_string(configuration_object.get("command"sv).value().as_string()));
    return {};
}

static ErrorOr<void> deploy_container_based_on_config_file(StringView config_file_path)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath proc mount unshare exec fattr chown"));
    auto file = TRY(Core::File::open(config_file_path, Core::File::OpenMode::Read));

    bool pid_isolation = false;
    bool enforce_jail = false;
    String command;
    Optional<String> hostname_context_name {};
    JsonArray new_vfs_root_context_layout_creation_sequence {};

    TRY(extract_values_from_file(*file, command, new_vfs_root_context_layout_creation_sequence, pid_isolation, hostname_context_name, enforce_jail));

    // NOTE: First we gather all information, then we start deploying.
    // To ensure proper functionality, we do this in the following sequence:
    // - Create PID isolation and attach the scoped process list
    // - Creating a VFS root context
    // - Populating the VFS root context with the desired layout
    // - Attach to the VFS root context
    // - Attach to the hostname context
    // - Enforce jail restrictions

    if (pid_isolation)
        TRY(create_and_attach_scoped_process_list_unshare());

    // NOTE: To be able to properly create the desired layout of the container,
    // we first mount a new filesystem instance in a temporary location, then we
    // populate its directories and mount subsequent mounts if so desired.
    // Then we copy all mounts (from the root directory of the VFS root context)
    // up to all of its subsequent mounts, completing the filesystem skeleton of the
    // container.
    if (!new_vfs_root_context_layout_creation_sequence.is_empty())
        TRY(create_custom_vfs_root_context_layout(new_vfs_root_context_layout_creation_sequence));

    // Remove the fattr & chown pledges
    TRY(Core::System::pledge("stdio rpath wpath cpath proc mount unshare exec"));

    if (hostname_context_name.has_value())
        TRY(create_and_attach_hostname_context(hostname_context_name.value().bytes_as_string_view()));

    // Remove the unshare pledge
    TRY(Core::System::pledge("stdio rpath wpath cpath proc mount exec"));

    if (enforce_jail)
        TRY(Core::System::enter_jail_mode_until_exit());

    // Remove the proc pledge
    TRY(Core::System::pledge("stdio rpath wpath cpath mount exec"));

    // FIXME: Find a better way to convert between String and Vector<StringView>
    auto splitted_command = command.bytes_as_string_view().split_view(' ');
    TRY(Core::System::exec_command(splitted_command, false));
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool pid_isolation = false;
    bool enforce_jail = false;
    bool preserve_env = false;

    Vector<StringView> command;
    StringView config_file;

    Core::ArgsParser args_parser;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_option(pid_isolation, "Create new process list", "pid-isolation", 'p');
    args_parser.add_option(config_file, "Use JSON-based configruation file", "configuration", 'f', "");
    args_parser.add_option(enforce_jail, "Enforce jail restrictions on container", "enforce-jail", 'j');
    args_parser.add_option(preserve_env, "Preserve user environment when running command", "preserve-env", 'E');
    args_parser.add_positional_argument(command, "Command to run at elevated privilege level", "command", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!config_file.is_null() && !config_file.is_empty()) {
        TRY(deploy_container_based_on_config_file(config_file));
        return 0;
    }

    TRY(Core::System::pledge("stdio rpath wpath cpath proc unshare exec"));

    if (command.is_empty())
        return Error::from_string_view("Can't create a container with no specified command."sv);

    if (!(pid_isolation || enforce_jail))
        return Error::from_string_view("Can't create a container with no attributes (jail/pid-isolation)."sv);

    if (pid_isolation)
        TRY(create_and_attach_scoped_process_list_unshare());
    // Remove the unshare pledge
    TRY(Core::System::pledge("stdio rpath wpath cpath proc exec"));

    if (enforce_jail)
        TRY(Core::System::enter_jail_mode_until_exit());

    // Remove the proc pledge
    TRY(Core::System::pledge("stdio rpath wpath cpath exec"));

    TRY(Core::System::exec_command(command, preserve_env));
    return 0;
}
