/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <Kernel/Firmware/ACPI/AML/AST.h>

namespace Kernel::ACPI::AML {

class Namespace {
public:
    ErrorOr<void> add_level(Vector<StringView> const& path);
    ErrorOr<void> insert_node(Vector<StringView> const& path, NonnullRefPtr<ASTNode>);
    ErrorOr<NonnullRefPtr<ASTNode>> get_node(Vector<StringView> const& path) const;
    ErrorOr<NonnullRefPtr<ASTNode>> search_node(Vector<StringView> const& path, StringView name) const;
    bool contains_node(Vector<StringView> const& path) const;

private:
    struct Level {
        HashMap<NonnullOwnPtr<KString>, Level> sub_levels;
        // FIXME: Storing the AST nodes directly works fine for the parsing stage,
        //  but once we start dynamically executing the bytecode, we will probably
        //  want to store some kind of dynamic value instead.
        HashMap<NonnullOwnPtr<KString>, NonnullRefPtr<ASTNode>> objects;
    };
    Level m_root_level;
};

}
