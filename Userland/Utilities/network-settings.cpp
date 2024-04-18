/*
 * Copyright (c) 2023, Fabian Dellwing <fabian@dellwing.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath recvfd sendfd proc exec"));

    TRY(Core::System::unveil("/bin/NetworkServer", "x"));
    TRY(Core::System::unveil("/etc/Network.ini", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto infile = TRY(Core::File::standard_input());

    auto input_bytes = TRY(infile->read_until_eof());
    StringView json_data = input_bytes;

    if (json_data.is_empty() || json_data.is_whitespace())
        return Error::from_errno(EINVAL);

    auto json = TRY(JsonParser(json_data).parse());

    if (!json.is_object())
        return Error::from_errno(EINVAL);

    auto json_object = json.as_object();

    auto config_file = TRY(Core::ConfigFile::open_for_system("Network", Core::ConfigFile::AllowWriting::Yes));
    json_object.for_each_member([&](ByteString const& adapter_name, JsonValue const& adapter_data) {
        adapter_data.as_object().for_each_member([&](ByteString const& key, JsonValue const& value) {
            switch (value.type()) {
            case JsonValue::Type::String:
                config_file->write_entry(adapter_name, key, value.as_string());
                break;
            case JsonValue::Type::Bool:
                config_file->write_bool_entry(adapter_name, key, value.as_bool());
                break;
            case JsonValue::Type::Null:
                break;
            default:
                dbgln("Extend switch/case key={}, value={}", key, value);
                VERIFY_NOT_REACHED();
            }
        });
    });
    TRY(config_file->sync());

    // FIXME: This should be done in a nicer way, but for that out NetworkServer implementation needs to actually be a server that we can talk to and not just a oneshot binary.
    auto command = Vector<StringView>();
    TRY(command.try_append("/bin/NetworkServer"sv));
    TRY(Core::System::exec_command(command, true));

    return 0;
}
