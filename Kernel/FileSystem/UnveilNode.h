/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Trie.h>
#include <Kernel/Library/KString.h>

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
    NonnullOwnPtr<KString> full_path;
    UnveilAccess permissions { None };
    bool explicitly_unveiled { false };

    UnveilMetadata(UnveilMetadata const&) = delete;
    UnveilMetadata(UnveilMetadata&&) = default;

    // Note: Intentionally not explicit.
    UnveilMetadata(NonnullOwnPtr<KString>&& full_path, UnveilAccess permissions = None, bool explicitly_unveiled = false)
        : full_path(move(full_path))
        , permissions(permissions)
        , explicitly_unveiled(explicitly_unveiled)
    {
    }

    ErrorOr<UnveilMetadata> copy() const
    {
        return UnveilMetadata {
            TRY(full_path->try_clone()),
            permissions,
            explicitly_unveiled,
        };
    }
};

struct UnveilNode final : public Trie<NonnullOwnPtr<KString>, UnveilMetadata, Traits<NonnullOwnPtr<KString>>, UnveilNode> {
    using Trie<NonnullOwnPtr<KString>, UnveilMetadata, Traits<NonnullOwnPtr<KString>>, UnveilNode>::Trie;

    bool was_explicitly_unveiled() const { return this->metadata_value().explicitly_unveiled; }
    UnveilAccess permissions() const { return this->metadata_value().permissions; }
    StringView path() const { return this->metadata_value().full_path->view(); }
};

}
