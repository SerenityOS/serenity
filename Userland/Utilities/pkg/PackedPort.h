/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AvailablePort.h"
#include "InstalledPortDatabase.h"
#include "Port.h"
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibSemVer/SemVer.h>

class PackedPort : public Port {
public:
    static ErrorOr<NonnullOwnPtr<PackedPort>> acquire_port_from_package_archive(LexicalPath const& port_archives_path, AvailablePort const& port);

    PackedPort(String const& name, String const& version, Vector<Port> const& dependencies)
        : Port(name, version)
        , m_dependencies(move(dependencies))
    {
    }

    PackedPort(String const& name, String const& version)
        : Port(name, version)
    {
    }

    Vector<Port> const& dependencies() const { return m_dependencies; }

    enum class ResolveAndInstallDependencies {
        Yes,
        No,
    };

    ErrorOr<void> manual_install(HashMap<String, AvailablePort> const& available_ports, InstalledPortDatabase& installed_ports_database, LexicalPath const& port_archives_path, LexicalPath const& root_path, ResolveAndInstallDependencies resolve_and_install_dependencies);
    ErrorOr<void> install(HashMap<String, AvailablePort> const& available_ports, InstalledPortDatabase& installed_ports_database, LexicalPath const& port_archives_path, LexicalPath const& root_path, ResolveAndInstallDependencies);
    void dump_details();

private:
    Vector<Port> m_dependencies;
};
