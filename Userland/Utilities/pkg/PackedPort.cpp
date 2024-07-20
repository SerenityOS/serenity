/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PackedPort.h"
#include "InstalledPort.h"
#include <AK/BufferedStream.h>
#include <AK/Function.h>
#include <AK/StringUtils.h>
#include <LibArchive/Tar.h>
#include <LibArchive/TarStream.h>
#include <LibCompress/Xz.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Directory.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>

static ErrorOr<void> decompress_port_package_into_tmp_pkg_directory(LexicalPath const& port_archives_path, LexicalPath const& port_package_path)
{
    auto file = TRY(Core::File::open(port_package_path.string(), Core::File::OpenMode::Read));
    auto cwd = TRY(Core::System::getcwd());
    TRY(Core::System::chdir(port_archives_path.string()));
    auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));
    auto input_stream = TRY(Compress::XzDecompressor::create(move(buffered_file)));

    TRY(Archive::TarInputStream::handle_input(move(input_stream), false, false, true));
    MUST(Core::System::chdir(cwd));
    return {};
}

struct PortDetails {
    String name;
    String version;
};

static ErrorOr<PortDetails> parse_packed_port_details_file(Core::InputBufferedFile& details_file, Vector<Port>& dependencies)
{
    auto buffer = TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));

    // NOTE: The first line should be "package {port_name} {port_version}"
    auto package_details_line = TRY(details_file.read_line(buffer));
    auto package_details_parts = package_details_line.split_view(' ');
    if (package_details_parts.size() != 3) {
        dbgln("Invalid package entry {} (only {} parts)", package_details_line, package_details_parts.size());
        return Error::from_string_view("Main package details line has invalid amount of parts"sv);
    }

    if (package_details_parts[0] != "package"sv) {
        dbgln("Invalid package entry {} has invalid identification \"{}\"", package_details_line, package_details_parts[0]);
        return Error::from_string_view("Main package details line has invalid identification"sv);
    }

    PortDetails details { TRY(String::from_utf8(package_details_parts[1])), TRY(String::from_utf8(package_details_parts[2])) };

    while (TRY(details_file.can_read_line())) {
        auto line = TRY(details_file.read_line(buffer));
        if (line.is_empty())
            continue;
        auto dependency_line_parts = line.split_view(' ');
        if (dependency_line_parts.size() != 3) {
            dbgln("Invalid package dependency entry {} (only {} parts)", line, dependency_line_parts.size());
            return Error::from_string_view("Package dependency line has invalid amount of parts"sv);
        }

        if (dependency_line_parts[0] != "package-dependency"sv) {
            dbgln("Invalid package dependency entry {} has invalid identification \"{}\"", dependency_line_parts, dependency_line_parts[0]);
            return Error::from_string_view("Package dependency line has invalid identification"sv);
        }

        TRY(dependencies.try_append(Port {
            TRY(String::from_utf8(dependency_line_parts[1])),
            TRY(String::from_utf8(dependency_line_parts[2])) }));
    }

    return details;
}

ErrorOr<void> PackedPort::manual_install(HashMap<String, AvailablePort> const& available_ports, InstalledPortDatabase& installed_ports_database, LexicalPath const& port_archives_path, LexicalPath const& root_path, ResolveAndInstallDependencies resolve_and_install_dependencies)
{
    TRY(install(available_ports, installed_ports_database, port_archives_path, root_path, resolve_and_install_dependencies));
    TRY(installed_ports_database.insert_new_port_to_ports_database(
        InstalledPort::Type::Manual,
        name(), InstalledPort {
                    name(),
                    version_string(),
                    InstalledPort::Type::Manual,
                },
        m_dependencies));
    outln("Installed {}-{}", name(), version_string());
    return {};
}

ErrorOr<void> PackedPort::install(HashMap<String, AvailablePort> const& available_ports, InstalledPortDatabase& installed_ports_database, LexicalPath const& port_archives_path, LexicalPath const& root_path, ResolveAndInstallDependencies resolve_and_install_dependencies)
{
    VERIFY(root_path.is_absolute());
    dbgln("pkg: Install {}-{} (root path is {})", name(), version_string(), root_path.string());
    dbgln("pkg: {}-{} has {} dependencies", name(), version_string(), m_dependencies.size());

    bool matched_port_not_found = true;
    for (auto& dependency : m_dependencies) {
        for (auto& installed_port : installed_ports_database.map()) {
            if (dependency.name() == installed_port.key && dependency.version_string() == installed_port.value.version_string()) {
                matched_port_not_found = false;
                break;
            }
        }
        if (matched_port_not_found) {
            if (resolve_and_install_dependencies == ResolveAndInstallDependencies::No) {
                warnln("pkg: Dependency requirement not fulfilled for {}", dependency.name());
                return Error::from_string_literal("Dependency requirement mismatch detected");
            } else {
                auto it = available_ports.find(dependency.name());
                if (it == available_ports.end())
                    return Error::from_string_literal("Port name mismatch in available ports list during recursive install");
                auto port = TRY(acquire_port_package_from_archive_file(port_archives_path, (*it).value));
                TRY(port->install(available_ports, installed_ports_database, port_archives_path, root_path, resolve_and_install_dependencies));
                TRY(installed_ports_database.insert_new_port_to_ports_database(
                    InstalledPort::Type::Auto,
                    dependency.name(), InstalledPort {
                                           dependency.name(),
                                           dependency.version_string(),
                                           InstalledPort::Type::Auto,
                                       },
                    port->dependencies()));
                outln("Installed {}-{}", dependency.name(), dependency.version_string());
            }
        }
    }
    return {};
}

void PackedPort::dump_details()
{
    outln("Package name: {}, version: {}", name(), version_string());
    outln("{}-{} has {} dependencies", name(), version_string(), m_dependencies.size());
    for (auto& dependency : m_dependencies) {
        outln("\t{}, version: {}", dependency.name(), dependency.version_string());
    }
}

ErrorOr<NonnullOwnPtr<PackedPort>> PackedPort::acquire_port_from_package_archive(LexicalPath const& port_archives_path, AvailablePort const& port)
{
    auto port_package_path = port_archives_path.append(TRY(String::formatted("{}-{}.tar.xz", port.name(), port.version_string())));
    TRY(decompress_port_package_into_tmp_pkg_directory(port_archives_path, port_package_path));
    auto extracted_package_directory_path = port_archives_path.append(TRY(String::formatted("{}-{}", port.name(), port.version_string())));

    if (Core::System::access(extracted_package_directory_path.string(), R_OK).is_error())
        return Error::from_string_literal("The directory on which the package was extracted is not accessible");

    Vector<Port> dependencies;
    auto package_details_file_path = extracted_package_directory_path.append("details"sv);
    auto package_details_file = TRY(Core::File::open(package_details_file_path.string(), Core::File::OpenMode::Read));
    auto buffered_file = TRY(Core::InputBufferedFile::create(move(package_details_file)));

    auto port_details = TRY(parse_packed_port_details_file(*buffered_file, dependencies));
    if (dependencies.is_empty())
        return adopt_nonnull_own_or_enomem(new (nothrow) PackedPort(port_details.name, port_details.version));
    return adopt_nonnull_own_or_enomem(new (nothrow) PackedPort(port_details.name, port_details.version, dependencies));
}
