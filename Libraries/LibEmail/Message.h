#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace LibEmail {

enum class Field : char {
    InternalDate,
    Flags,
    UID,
    Envelope,
    BodyText
};

enum class SystemFlag : char {
    Seen,
    Answered,
    Flagged,
    Deleted,
    Draft,
    Recent
};

// Header info. for emails in the Internet Message Format (RFC-2822)
class Envelope {
public:
    static Envelope create_from_imap_data(StringView);
    ~Envelope();

    StringView date() const { return m_date; }
    StringView subject() const { return m_subject; }
    StringView from() const { return m_from; }
    StringView to() const { return m_to; }

private:
    // The date of sending (can differ from Message's INTERNALDATE)
    String m_date;
    String m_subject;
    // The email address of the user who sent the email
    String m_from;
    // The email address of the user who received the email
    String m_to;
};

class Message {
public:
    static Message create_from_imap_data(StringView);
    ~Message();

    u32 uid() const { return m_uid; }
    const Vector<SystemFlag>& system_flags() const { return m_system_flags; }
    const Vector<String>& keyword_flags() const { return m_keyword_flags; }
    StringView internal_date() const { return m_internal_date; }
    const Envelope& envelope() const { return m_envelope; }
    StringView text() const { return m_text; }

private:
    void load_flags(StringView);

    // id representing a message; can't change within a session
    u32 m_uid;
    // Standardized flags
    Vector<SystemFlag> m_system_flags;
    // User-created flags; can't start with '/'
    Vector<String> m_keyword_flags;
    // Date format is undefined in IMAP spec
    String m_internal_date;
    // The header of the message as found in RFC-2822
    Envelope m_envelope;
    // The text of the message (value of BODY[TEXT] field)
    String m_text;
};

}
