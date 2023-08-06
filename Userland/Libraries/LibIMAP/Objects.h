/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Tuple.h>
#include <AK/Variant.h>
#include <LibCore/DateTime.h>
#include <LibCore/EventReceiver.h>

namespace IMAP {
enum class CommandType {
    Append,
    Authenticate,
    Capability,
    Copy,
    Check,
    Close,
    Create,
    Delete,
    Examine,
    Expunge,
    Fetch,
    Idle,
    List,
    ListSub,
    Login,
    Logout,
    Noop,
    Rename,
    Search,
    Select,
    Status,
    Store,
    Subscribe,
    UIDCopy,
    UIDFetch,
    UIDSearch,
    UIDStore,
    Unsubscribe,
};

enum class MailboxFlag : unsigned {
    All = 1u << 0,
    Drafts = 1u << 1,
    Flagged = 1u << 2,
    HasChildren = 1u << 3,
    HasNoChildren = 1u << 4,
    Important = 1u << 5,
    Junk = 1u << 6,
    Marked = 1u << 7,
    NoInferiors = 1u << 8,
    NoSelect = 1u << 9,
    Sent = 1u << 10,
    Trash = 1u << 11,
    Unmarked = 1u << 12,
    Unknown = 1u << 13,
};

enum class ResponseType : unsigned {
    Capability = 1u << 0,
    List = 1u << 1,
    Exists = 1u << 2,
    Recent = 1u << 3,
    Flags = 1u << 4,
    UIDNext = 1u << 5,
    UIDValidity = 1u << 6,
    Unseen = 1u << 7,
    PermanentFlags = 1u << 8,
    Fetch = 1u << 9,
    Search = 1u << 10,
    ListSub = 1u << 11,
    Expunged = 1u << 12,
    Bye = 1u << 13,
    Status = 1u << 14
};

enum class FetchResponseType : unsigned {
    Body = 1u << 1,
    UID = 1u << 2,
    InternalDate = 1u << 3,
    Envelope = 1u << 4,
    Flags = 1u << 5,
    BodyStructure = 1u << 6,
};

enum class StatusItemType : unsigned {
    Recent = 1u << 1,
    UIDNext = 1u << 2,
    UIDValidity = 1u << 3,
    Unseen = 1u << 4,
    Messages = 1u << 5,
};

class Parser;

class StatusItem {
public:
    [[nodiscard]] unsigned status_items() const
    {
        return m_status_items;
    }

    [[nodiscard]] bool contains_status_item_type(StatusItemType type) const
    {
        return (static_cast<unsigned>(type) & m_status_items) != 0;
    }

    void add_status_item_type(StatusItemType type)
    {
        m_status_items |= static_cast<unsigned>(type);
    }

    void set_mailbox(DeprecatedString&& mailbox) { m_mailbox = move(mailbox); }
    DeprecatedString& mailbox() { return m_mailbox; }

    unsigned get(StatusItemType type) const
    {
        VERIFY(contains_status_item_type(type));
        switch (type) {
        case StatusItemType::Recent:
            return m_recent;
        case StatusItemType::UIDNext:
            return m_uid_next;
        case StatusItemType::UIDValidity:
            return m_uid_validity;
        case StatusItemType::Unseen:
            return m_unseen;
        case StatusItemType::Messages:
            return m_messages;
        }
        VERIFY_NOT_REACHED();
    }

    void set(StatusItemType type, unsigned value)
    {
        add_status_item_type(type);
        switch (type) {
        case StatusItemType::Recent:
            m_recent = value;
            break;
        case StatusItemType::UIDNext:
            m_uid_next = value;
            break;
        case StatusItemType::UIDValidity:
            m_uid_validity = value;
            break;
        case StatusItemType::Unseen:
            m_unseen = value;
            break;
        case StatusItemType::Messages:
            m_uid_next = value;
            break;
        }
    }

private:
    unsigned m_status_items { 0 };
    unsigned m_messages { 0 };
    unsigned m_recent { 0 };
    unsigned m_uid_next { 0 };
    unsigned m_uid_validity { 0 };
    unsigned m_unseen { 0 };
    DeprecatedString m_mailbox;
};

struct Address {
    Optional<DeprecatedString> name;
    Optional<DeprecatedString> source_route;
    Optional<DeprecatedString> mailbox;
    Optional<DeprecatedString> host;
};

struct Envelope {
    Optional<DeprecatedString> date; // Format of date not specified.
    Optional<DeprecatedString> subject;
    Optional<Vector<Address>> from;
    Optional<Vector<Address>> sender;
    Optional<Vector<Address>> reply_to;
    Optional<Vector<Address>> to;
    Optional<Vector<Address>> cc;
    Optional<Vector<Address>> bcc;
    Optional<DeprecatedString> in_reply_to;
    Optional<DeprecatedString> message_id;
};

class BodyStructure;

struct BodyExtension {
    AK::Variant<Optional<DeprecatedString>, unsigned, Vector<OwnPtr<BodyExtension>>> data;
};

struct MultiPartBodyStructureData {
    Optional<Tuple<DeprecatedString, HashMap<DeprecatedString, DeprecatedString>>> disposition;
    Vector<OwnPtr<BodyStructure>> bodies;
    Vector<DeprecatedString> langs;
    DeprecatedString media_type;
    Optional<HashMap<DeprecatedString, DeprecatedString>> params;
    Optional<DeprecatedString> location;
    Optional<Vector<BodyExtension>> extensions;
};

struct BodyStructureData {
    DeprecatedString type;
    DeprecatedString subtype;
    Optional<DeprecatedString> id {};
    Optional<DeprecatedString> desc {};
    DeprecatedString encoding;
    HashMap<DeprecatedString, DeprecatedString> fields;
    unsigned bytes { 0 };
    unsigned lines { 0 };
    Optional<Envelope> envelope;

    Optional<DeprecatedString> md5 {};
    Optional<Tuple<DeprecatedString, HashMap<DeprecatedString, DeprecatedString>>> disposition {};
    Optional<Vector<DeprecatedString>> langs {};
    Optional<DeprecatedString> location {};

    Optional<Vector<BodyExtension>> extensions {};
};

class BodyStructure {
    friend Parser;

public:
    explicit BodyStructure(BodyStructureData&& data)
        : m_data(move(data))
    {
    }

    explicit BodyStructure(MultiPartBodyStructureData&& data)
        : m_data(move(data))
    {
    }

    AK::Variant<BodyStructureData, MultiPartBodyStructureData> const& data() const { return m_data; }

private:
    AK::Variant<BodyStructureData, MultiPartBodyStructureData> m_data;
};

// Set -1 for '*' i.e highest possible value.
struct Sequence {
    int start;
    int end;

    [[nodiscard]] DeprecatedString serialize() const;
};

struct FetchCommand {
    enum class DataItemType {
        BodyStructure,
        Envelope,
        Flags,
        InternalDate,
        UID,
        PeekBody,
        BodySection
    };

    struct DataItem {
        enum class SectionType {
            Header,
            HeaderFields,
            HeaderFieldsNot,
            Text,
            Parts
        };
        struct Section {
            SectionType type;

            Optional<Vector<unsigned>> parts {};
            bool ends_with_mime {};

            Optional<Vector<DeprecatedString>> headers {};

            [[nodiscard]] DeprecatedString serialize() const;
        };

        DataItemType type;

        Optional<Section> section {};
        bool partial_fetch { false };
        int start { 0 };
        int octets { 0 };

        [[nodiscard]] DeprecatedString serialize() const;
    };

    Vector<Sequence> sequence_set;
    Vector<DataItem> data_items;

    DeprecatedString serialize();
};
struct Command {
public:
    CommandType type;
    int tag;
    Vector<DeprecatedString> args;
};

enum class ResponseStatus {
    Bad,
    No,
    OK,
};

struct ListItem {
    unsigned flags;
    DeprecatedString reference;
    DeprecatedString name;
};

class FetchResponseData {
public:
    [[nodiscard]] unsigned response_type() const
    {
        return m_response_type;
    }

    [[nodiscard]] bool contains_response_type(FetchResponseType response_type) const
    {
        return (static_cast<unsigned>(response_type) & m_response_type) != 0;
    }

    void add_response_type(FetchResponseType type)
    {
        m_response_type |= static_cast<unsigned>(type);
    }

    void add_body_data(FetchCommand::DataItem&& data_item, Optional<DeprecatedString>&& body)
    {
        add_response_type(FetchResponseType::Body);
        m_bodies.append({ move(data_item), move(body) });
    }

    Vector<Tuple<FetchCommand::DataItem, Optional<DeprecatedString>>>& body_data()
    {
        VERIFY(contains_response_type(FetchResponseType::Body));
        return m_bodies;
    }

    void set_uid(unsigned uid)
    {
        add_response_type(FetchResponseType::UID);
        m_uid = uid;
    }

    [[nodiscard]] unsigned uid() const
    {
        VERIFY(contains_response_type(FetchResponseType::UID));
        return m_uid;
    }

    void set_internal_date(Core::DateTime time)
    {
        add_response_type(FetchResponseType::InternalDate);
        m_internal_date = time;
    }

    Core::DateTime& internal_date()
    {
        VERIFY(contains_response_type(FetchResponseType::InternalDate));
        return m_internal_date;
    }

    void set_envelope(Envelope&& envelope)
    {
        add_response_type(FetchResponseType::Envelope);
        m_envelope = move(envelope);
    }

    Envelope& envelope()
    {
        VERIFY(contains_response_type(FetchResponseType::Envelope));
        return m_envelope;
    }

    void set_flags(Vector<DeprecatedString>&& flags)
    {
        add_response_type(FetchResponseType::Flags);
        m_flags = move(flags);
    }

    Vector<DeprecatedString>& flags()
    {
        VERIFY(contains_response_type(FetchResponseType::Flags));
        return m_flags;
    }

    void set_body_structure(BodyStructure&& structure)
    {
        add_response_type(FetchResponseType::BodyStructure);
        m_body_structure = move(structure);
    }

    BodyStructure& body_structure()
    {
        VERIFY(contains_response_type(FetchResponseType::BodyStructure));
        return m_body_structure;
    }

    FetchResponseData()
        : m_body_structure(BodyStructureData {})
    {
    }

private:
    Vector<DeprecatedString> m_flags;
    Vector<Tuple<FetchCommand::DataItem, Optional<DeprecatedString>>> m_bodies;
    Core::DateTime m_internal_date;
    Envelope m_envelope;
    unsigned m_uid { 0 };
    unsigned m_response_type { 0 };
    BodyStructure m_body_structure;
};

DeprecatedString serialize_astring(StringView string);

struct SearchKey {
public:
    // clang-format off
    struct All { };
    struct Answered { };
    struct Bcc { DeprecatedString bcc; };
    struct Cc { DeprecatedString cc; };
    struct Deleted { };
    struct Draft { };
    struct From { DeprecatedString from; };
    struct Header { DeprecatedString header; DeprecatedString value; };
    struct Keyword { DeprecatedString keyword; };
    struct Larger { unsigned number; };
    struct New { };
    struct Not { OwnPtr<SearchKey> operand; };
    struct Old { };
    struct On { Core::DateTime date; };
    struct Or { OwnPtr<SearchKey> lhs; OwnPtr<SearchKey> rhs; };
    struct Recent { };
    struct SearchKeys { Vector<OwnPtr<SearchKey>> keys; };
    struct Seen { };
    struct SentBefore { Core::DateTime date; };
    struct SentOn { Core::DateTime date; };
    struct SentSince { Core::DateTime date; };
    struct SequenceSet { Sequence sequence; };
    struct Since { Core::DateTime date; };
    struct Smaller { unsigned number; };
    struct Subject { DeprecatedString subject; };
    struct Text { DeprecatedString text; };
    struct To { DeprecatedString to; };
    struct UID { unsigned  uid; };
    struct Unanswered { };
    struct Undeleted { };
    struct Undraft { };
    struct Unkeyword { DeprecatedString flag_keyword; };
    struct Unseen { };
    // clang-format on

    Variant<All, Answered, Bcc, Cc, Deleted, Draft, From, Header, Keyword,
        Larger, New, Not, Old, On, Or, Recent, SearchKeys, Seen, SentBefore, SentOn,
        SentSince, SequenceSet, Since, Smaller, Subject, Text, To, UID, Unanswered,
        Undeleted, Undraft, Unkeyword, Unseen>
        data;

    SearchKey(SearchKey&& other) noexcept
        : data(move(other.data))
    {
    }

    template<typename T>
    explicit SearchKey(T&& t)
        : data(forward<T>(t))
    {
    }

    SearchKey& operator=(SearchKey&& other) noexcept
    {
        if (this == &other) {
            return *this;
        }

        this->data = move(other.data);

        return *this;
    }

    [[nodiscard]] DeprecatedString serialize() const;
};

class ResponseData {
public:
    [[nodiscard]] unsigned response_type() const
    {
        return m_response_type;
    }

    ResponseData()
        : m_response_type(0)
    {
    }

    ResponseData(ResponseData&) = delete;
    ResponseData(ResponseData&&) = default;
    ResponseData& operator=(ResponseData const&) = delete;
    ResponseData& operator=(ResponseData&&) = default;

    [[nodiscard]] bool contains_response_type(ResponseType response_type) const
    {
        return (static_cast<unsigned>(response_type) & m_response_type) != 0;
    }

    void add_response_type(ResponseType response_type)
    {
        m_response_type = m_response_type | static_cast<unsigned>(response_type);
    }

    void add_capabilities(Vector<DeprecatedString>&& capabilities)
    {
        m_capabilities = move(capabilities);
        add_response_type(ResponseType::Capability);
    }

    Vector<DeprecatedString>& capabilities()
    {
        VERIFY(contains_response_type(ResponseType::Capability));
        return m_capabilities;
    }

    void add_list_item(ListItem&& item)
    {
        add_response_type(ResponseType::List);
        m_list_items.append(move(item));
    }

    Vector<ListItem>& list_items()
    {
        VERIFY(contains_response_type(ResponseType::List));
        return m_list_items;
    }

    void add_lsub_item(ListItem&& item)
    {
        add_response_type(ResponseType::List);
        m_lsub_items.append(move(item));
    }

    Vector<ListItem>& lsub_items()
    {
        VERIFY(contains_response_type(ResponseType::ListSub));
        return m_lsub_items;
    }

    void set_exists(unsigned exists)
    {
        add_response_type(ResponseType::Exists);
        m_exists = exists;
    }

    [[nodiscard]] unsigned exists() const
    {
        VERIFY(contains_response_type(ResponseType::Exists));
        return m_exists;
    }

    void set_recent(unsigned recent)
    {
        add_response_type(ResponseType::Recent);
        m_recent = recent;
    }

    [[nodiscard]] unsigned recent() const
    {
        VERIFY(contains_response_type(ResponseType::Recent));
        return m_recent;
    }

    void set_uid_next(unsigned uid_next)
    {
        add_response_type(ResponseType::UIDNext);
        m_uid_next = uid_next;
    }

    [[nodiscard]] unsigned uid_next() const
    {
        VERIFY(contains_response_type(ResponseType::UIDNext));
        return m_uid_next;
    }

    void set_uid_validity(unsigned uid_validity)
    {
        add_response_type(ResponseType::UIDValidity);
        m_uid_validity = uid_validity;
    }

    [[nodiscard]] unsigned uid_validity() const
    {
        VERIFY(contains_response_type(ResponseType::UIDValidity));
        return m_uid_validity;
    }

    void set_unseen(unsigned unseen)
    {
        add_response_type(ResponseType::Unseen);
        m_unseen = unseen;
    }

    [[nodiscard]] unsigned unseen() const
    {
        VERIFY(contains_response_type(ResponseType::Unseen));
        return m_unseen;
    }

    void set_flags(Vector<DeprecatedString>&& flags)
    {
        m_response_type |= static_cast<unsigned>(ResponseType::Flags);
        m_flags = move(flags);
    }

    Vector<DeprecatedString>& flags()
    {
        VERIFY(contains_response_type(ResponseType::Flags));
        return m_flags;
    }

    void set_permanent_flags(Vector<DeprecatedString>&& flags)
    {
        add_response_type(ResponseType::PermanentFlags);
        m_permanent_flags = move(flags);
    }

    Vector<DeprecatedString>& permanent_flags()
    {
        VERIFY(contains_response_type(ResponseType::PermanentFlags));
        return m_permanent_flags;
    }

    void add_fetch_response(unsigned message, FetchResponseData&& data)
    {
        add_response_type(ResponseType::Fetch);
        m_fetch_responses.append(Tuple<unsigned, FetchResponseData> { move(message), move(data) });
    }

    Vector<Tuple<unsigned, FetchResponseData>>& fetch_data()
    {
        VERIFY(contains_response_type(ResponseType::Fetch));
        return m_fetch_responses;
    }

    void set_search_results(Vector<unsigned>&& results)
    {
        add_response_type(ResponseType::Search);
        m_search_results = move(results);
    }

    Vector<unsigned>& search_results()
    {
        VERIFY(contains_response_type(ResponseType::Search));
        return m_search_results;
    }

    void add_expunged(unsigned message)
    {
        add_response_type(ResponseType::Expunged);
        m_expunged.append(message);
    }

    Vector<unsigned>& expunged()
    {
        VERIFY(contains_response_type(ResponseType::Expunged));
        return m_expunged;
    }

    void set_bye(Optional<DeprecatedString> message)
    {
        add_response_type(ResponseType::Bye);
        m_bye_message = move(message);
    }

    Optional<DeprecatedString>& bye_message()
    {
        VERIFY(contains_response_type(ResponseType::Bye));
        return m_bye_message;
    }

    void set_status(StatusItem&& status_item)
    {
        add_response_type(ResponseType::Status);
        m_status_item = move(status_item);
    }

    StatusItem& status_item()
    {
        return m_status_item;
    }

private:
    unsigned m_response_type;

    Vector<DeprecatedString> m_capabilities;
    Vector<ListItem> m_list_items;
    Vector<ListItem> m_lsub_items;
    Vector<unsigned> m_expunged;

    unsigned m_recent {};
    unsigned m_exists {};

    unsigned m_uid_next {};
    unsigned m_uid_validity {};
    unsigned m_unseen {};
    Vector<DeprecatedString> m_permanent_flags;
    Vector<DeprecatedString> m_flags;
    Vector<Tuple<unsigned, FetchResponseData>> m_fetch_responses;
    Vector<unsigned> m_search_results;
    Optional<DeprecatedString> m_bye_message;
    StatusItem m_status_item;
};

enum class StoreMethod {
    Replace,
    Add,
    Remove
};

class SolidResponse {
    // Parser is allowed to set up fields
    friend class Parser;

public:
    ResponseStatus status() { return m_status; }

    int tag() const { return m_tag; }

    ResponseData& data() { return m_data; }

    DeprecatedString response_text() { return m_response_text; }

    SolidResponse()
        : SolidResponse(ResponseStatus::Bad, -1)
    {
    }

    SolidResponse(ResponseStatus status, int tag)
        : m_status(status)
        , m_tag(tag)
        , m_data(ResponseData())
    {
    }

private:
    ResponseStatus m_status;
    DeprecatedString m_response_text;
    unsigned m_tag;

    ResponseData m_data;
};

struct ContinueRequest {
    DeprecatedString data;
};

using Response = Variant<SolidResponse, ContinueRequest>;
}

// An RFC 2822 message
// https://datatracker.ietf.org/doc/html/rfc2822
struct Message {
    DeprecatedString data;
};
