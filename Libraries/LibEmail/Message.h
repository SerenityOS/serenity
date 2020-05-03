/*
 * Copyright (c) 2020, Cole Blakley <cblakley15@gmail.com>
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
