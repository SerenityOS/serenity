/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
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

#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/URL.h>
#include <LibCore/ConfigFile.h>

namespace LaunchServer {

struct Handler {
    String name;
    String executable;
    HashTable<String> file_types;
    HashTable<String> protocols;
};

class Launcher {
public:
    Launcher();
    static Launcher& the();

    void load_handlers(const String& af_dir);
    void load_config(const Core::ConfigFile&);
    bool open_url(const URL&, const String& handler_name);
    Vector<String> handlers_for_url(const URL&);

private:
    HashMap<String, Handler> m_handlers;
    HashMap<String, String> m_protocol_handlers;
    HashMap<String, String> m_file_handlers;

    Vector<String> handlers_for(const String& key, HashMap<String, String>& user_preferences, Function<bool(Handler&, const String&)> handler_matches);
    Vector<String> handlers_for_path(const String&);
    bool open_file_url(const URL&);
    bool open_with_user_preferences(const HashMap<String, String>& user_preferences, const String key, const String argument, const String default_program);
    bool open_with_handler_name(const URL&, const String& handler_name);
};
}
