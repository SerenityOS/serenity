/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AvailablePort.h"
#include "AvailablePortDatabase.h"
#include "InstalledPort.h"
#include "InstalledPortDatabase.h"
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

    TRY(Core::System::unveil("/tmp/session/%sid/portal/request", "rw"));
    TRY(Core::System::unveil("/usr"sv, "c"sv));
    TRY(Core::System::unveil("/usr/Ports"sv, "rwc"sv));
    TRY(Core::System::unveil("/res"sv, "r"sv));
    TRY(Core::System::unveil("/usr/lib"sv, "r"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool verbose = false;
    bool show_all_installed_ports = false;
    bool update_packages_db = false;
    StringView query_package {};

    Core::ArgsParser args_parser;
    args_parser.add_option(show_all_installed_ports, "Show all manually-installed ports", "list-manual-ports", 'l');
    args_parser.add_option(update_packages_db, "Sync/Update ports database", "update-ports-database", 'u');
    args_parser.add_option(query_package, "Query ports database for package name", "query-package", 'q', "Package name to query");
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.parse(arguments);

    if (!update_packages_db && !show_all_installed_ports && query_package.is_null()) {
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

    if (Core::System::access(InstalledPortDatabase::default_path, R_OK).is_error()) {
        warnln("pkg: {} isn't accessible, did you install a package in the past?", InstalledPortDatabase::default_path);
        return 1;
    }

    auto installed_ports_database = TRY(InstalledPortDatabase::instantiate_ports_database(InstalledPortDatabase::default_path));

    if (Core::System::access(AvailablePortDatabase::default_path, R_OK).is_error()) {
        outln("pkg: Please run this program with -u first!");
        return 0;
    }
    auto available_ports_database = TRY(AvailablePortDatabase::instantiate_ports_database(AvailablePortDatabase::default_path));

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
