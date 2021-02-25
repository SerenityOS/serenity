/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
