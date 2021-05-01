/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Trie.h>

namespace Kernel {

enum UnveilAccess {
    Read = 1,
    Write = 2,
    Execute = 4,
    CreateOrRemove = 8,
    Browse = 16,

    None = 0,
};

struct UnveilNode;

struct UnveilMetadata {
    String full_path;
    UnveilAccess permissions { None };
    bool explicitly_unveiled { false };
    bool unveil_inherited_from_root { false }; // true if permissions are inherited from the tree root (/).
};

struct UnveilNode final : public Trie<String, UnveilMetadata, Traits<String>, UnveilNode> {
    using Trie<String, UnveilMetadata, Traits<String>, UnveilNode>::Trie;

    bool permissions_inherited_from_root() const { return this->metadata_value().unveil_inherited_from_root; }
    bool was_explicitly_unveiled() const { return this->metadata_value().explicitly_unveiled; }
    UnveilAccess permissions() const { return this->metadata_value().permissions; }
    const String& path() const { return this->metadata_value().full_path; }
};

}
