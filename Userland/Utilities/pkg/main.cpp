/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AvailablePort.h"
#include "AvailablePortDatabase.h"
#include "InstalledPort.h"
#include "InstalledPortDatabase.h"
#include "PackedPort.h"
#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

static void print_port_details(InstalledPort const& port, Optional<AvailablePort> const& available_port)
{
    out("{}, installed as {}, version {}", port.name(), port.type_as_string_view(), port.version_string());
    if (available_port.has_value()) {
        auto const& upstream_port = available_port.value();
        auto const& upstream_version = upstream_port.version_semver();
        auto const& this_version = port.version_semver();

        if ((this_version.is_error() || upstream_version.is_error())) {
            if (upstream_port.version_string() != port.version_string()) {
                outln(" (upgrade available -> {})", upstream_port.version_string());
            }
        } else {
            auto const& ap_version = upstream_version.value();
            auto const& ip_version = this_version.value();
            if (ip_version.is_same(ap_version)) {
                outln(" (already on latest version)");
            } else if (ip_version.is_lesser_than(ap_version)) {
                outln(" (upgrade available {})", ap_version.to_string());
            }
        }
    } else {
        outln();
    }

    if (!port.dependencies().is_empty()) {
        out("    Dependencies:");
        for (auto const& dependency : port.dependencies())
            out(" {}", dependency);
        outln();
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd thread unix rpath cpath wpath"));

    bool verbose = false;
    bool show_all_installed_ports = false;
    bool install_port = false;
    bool delete_port = false;
    bool update_packages_db = false;
    bool recursively_install_packages = false;
    StringView query_package {};

    Vector<StringView> names;

    Optional<StringView> root_path_param;
    Optional<StringView> port_archives_path_param;

    Core::ArgsParser args_parser;
    args_parser.add_option(show_all_installed_ports, "Show all manually-installed ports", "list-manual-ports", 'l');
    args_parser.add_option(install_port, "Install port", "install-port", 'i');
    args_parser.add_option(delete_port, "Delete port", "delete-port", 'd');
    args_parser.add_option(recursively_install_packages, "Try to resolve and install dependencies recuresively", "recursively-install-packages", 'R');
    args_parser.add_option(root_path_param, "Use another root path as a base path when handling ports", "root-path", 'B', "Work root path");
    args_parser.add_option(port_archives_path_param, "Use another port archives path path", "port-archives-path", 'b', "Port archives path");
    args_parser.add_option(update_packages_db, "Sync/Update ports database", "update-ports-database", 'u');
    args_parser.add_option(query_package, "Query ports database for package name", "query-package", 'q', "Package name to query");
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.add_positional_argument(names, "Names (paths, port names)", "names", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    LexicalPath root_path((root_path_param.has_value()
                              && !root_path_param.value().is_null()
                              && !root_path_param.value().is_empty())
            ? root_path_param.value()
            : "/"sv);
    LexicalPath port_archives_path((port_archives_path_param.has_value()
                                       && !port_archives_path_param.value().is_null()
                                       && !port_archives_path_param.value().is_empty())
            ? port_archives_path_param.value()
            : "/tmp/pkg"sv);
    if (!root_path.is_absolute())
        return Error::from_string_literal("Root port path should be an absolute path");

    if (!port_archives_path.is_absolute())
        return Error::from_string_literal("Port archives path should be an absolute path");

    if (install_port && delete_port) {
        warnln("pkg: Can't install and delete ports at once.");
        return 1;
    }

    if (!install_port && !&&!update_packages_db && !show_all_installed_ports && query_package.is_null()) {
        outln("pkg: No action to be performed was specified.");
        return 0;
    }

    int return_value = 0;
    if (update_packages_db) {
        if (getuid() != 0) {
            outln("pkg: Requires root to update packages database.");
            return 1;
        }
        return_value = TRY(AvailablePortDatabase::download_available_ports_list_file(AvailablePortDatabase::default_path));
    }

    auto installed_ports_database_path = root_path.append(InstalledPortDatabase::default_path).string();
    if (Core::System::access(installed_ports_database_path, R_OK).is_error()) {
        warnln("pkg: {} isn't accessible, did you install a package in the past?", installed_ports_database_path);
        return 1;
    }

    auto installed_ports_database = TRY(InstalledPortDatabase::instantiate_ports_database(installed_ports_database_path));

    auto available_ports_database_path = root_path.append(AvailablePortDatabase::default_path).string();
    if (Core::System::access(available_ports_database_path, R_OK).is_error()) {
        outln("pkg: Please run this program with -u first!");
        return 0;
    }
    auto available_ports_database = TRY(AvailablePortDatabase::instantiate_ports_database(available_ports_database_path));

    if (install_port) {
        if (getuid() != 0) {
            outln("pkg: Requires root to install ports.");
            return 1;
        }
        if (names.is_empty()) {
            outln("pkg: No port package paths being specified.");
            return 1;
        }

        // FIXME: Add a method to download port scripts over the Internet
        // to install a port which we don't have a local package for!
        for (auto& port_package_name : names) {
            auto it = available_ports.find(port_package_name);
            if (it == available_ports.end())
                return Error::from_string_literal("Port name mismatch in available ports list");

            auto& port_to_be_installed = *it;
            VERIFY(port_to_be_installed.value.name() == port_package_name);
            auto port_package = TRY(PackedPort::acquire_port_from_package_archive(port_archives_path, port_to_be_installed.value));
            TRY(port_package->manual_install(available_ports, *installed_ports_database,
                port_archives_path,
                root_path,
                recursively_install_packages ? PackedPort::ResolveAndInstallDependencies::Yes : PackedPort::ResolveAndInstallDependencies::No));
        }
        return 0;
    }

    if (show_all_installed_ports) {
        outln("Manually-installed ports:");
        auto& database = *available_ports_database;
        TRY(installed_ports_database->for_each_by_type(InstalledPort::Type::Manual, [database](InstalledPort const& port) -> ErrorOr<void> {
            auto available_port = database.map().find(port.name());
            if (available_port != database.map().end()) {
                print_port_details(port, available_port->value);
            } else {
                print_port_details(port, {});
            }
            return {};
        }));
    }

    if (!query_package.is_null()) {
        if (query_package.is_empty()) {
            outln("pkg: Queried package name is empty.");
            return 0;
        }
        available_ports_database->query_details_for_package(installed_ports_database->map(), query_package, verbose);
    }

    return return_value;
}
