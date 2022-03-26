/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibXML/FundamentalTypes.h>

namespace XML {

struct ElementDeclaration {
    struct Empty {
    };
    struct Any {
    };
    struct Mixed {
        HashTable<Name> types;
        bool many;
    };
    struct Children {
        struct Entry;
        enum class Qualifier {
            ExactlyOnce,
            Optional,
            Any,
            OneOrMore,
        };

        struct Choice {
            Vector<Entry> entries;
            Qualifier qualifier;
        };
        struct Sequence {
            Vector<Entry> entries;
            Qualifier qualifier;
        };

        struct Entry {
            Variant<Name, Choice, Sequence> sub_entries;
            Qualifier qualifier;
        };

        Variant<Choice, Sequence> contents;
        Qualifier qualifier;
    };
    using ContentSpec = Variant<Empty, Any, Mixed, Children>;

    Name type;
    ContentSpec content_spec;
};

struct AttributeListDeclaration {
    enum class StringType {
        CData,
    };
    enum class TokenizedType {
        ID,
        IDRef,
        IDRefs,
        Entity,
        Entities,
        NMToken,
        NMTokens,
    };
    struct NotationType {
        HashTable<Name> names;
    };
    struct Enumeration {
        // FIXME: NMToken
        HashTable<String> tokens;
    };
    using Type = Variant<StringType, TokenizedType, NotationType, Enumeration>;

    struct Required {
    };
    struct Implied {
    };
    struct Fixed {
        String value;
    };
    struct DefaultValue {
        String value;
    };

    using Default = Variant<Required, Implied, Fixed, DefaultValue>;

    struct Definition {
        Name name;
        Type type;
        Default default_;
    };
    Name type;
    Vector<Definition> attributes;
};

struct PublicID {
    String public_literal;
};

struct SystemID {
    String system_literal;
};

struct ExternalID {
    Optional<PublicID> public_id;
    SystemID system_id;
};

struct EntityDefinition {
    ExternalID id;
    Optional<Name> notation;
};

struct GEDeclaration {
    Name name;
    Variant<String, EntityDefinition> definition;
};

struct PEDeclaration {
    Name name;
    Variant<String, ExternalID> definition;
};

using EntityDeclaration = Variant<GEDeclaration, PEDeclaration>;

struct NotationDeclaration {
    Name name;
    Variant<ExternalID, PublicID> notation;
};

using MarkupDeclaration = Variant<ElementDeclaration, AttributeListDeclaration, EntityDeclaration, NotationDeclaration>;
}
