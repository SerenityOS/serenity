/*
 * Copyright (c) 2020, Peter Elliott <pelliott@ualberta.ca>
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

#include <AK/Result.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <pwd.h>
#include <sys/types.h>

namespace Core {

class Account {
public:
    static Result<Account, String> from_name(const char* username);
    static Result<Account, String> from_uid(uid_t uid);

    bool authenticate(const char* password) const;
    bool login() const;

    String username() const { return m_username; }
    String password_hash() const { return m_password_hash; }

    // Setters only affect in-memory copy of password.
    // You must call sync to apply changes.
    void set_password(const char* password);
    void set_password_enabled(bool enabled);
    void delete_password();
    bool has_password() const { return !m_password_hash.is_empty(); }

    uid_t uid() const { return m_uid; }
    gid_t gid() const { return m_gid; }
    const String& gecos() const { return m_gecos; }
    const String& home_directory() const { return m_home_directory; }
    const String& shell() const { return m_shell; }
    const Vector<gid_t>& extra_gids() const { return m_extra_gids; }

    bool sync();

private:
    Account(struct passwd* pwd, Vector<gid_t> extra_gids);

    String m_username;

    // Contents of passwd field in passwd entry.
    // Can be empty, "x", or contain a leading '!'
    String m_password_hash;
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    String m_gecos;
    String m_home_directory;
    String m_shell;
    Vector<gid_t> m_extra_gids;
};

}
