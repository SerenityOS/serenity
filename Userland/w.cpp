#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/File.h>
#include <pwd.h>
#include <stdio.h>
#include <sys/stat.h>
#include <time.h>

int main()
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/dev", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/var/run/utmp", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto file_or_error = Core::File::open("/var/run/utmp", Core::IODevice::ReadOnly);
    if (file_or_error.is_error()) {
        warn() << "Error: " << file_or_error.error();
        return 1;
    }
    auto& file = *file_or_error.value();
    auto json = JsonValue::from_string(file.read_all());
    if (!json.has_value() || !json.value().is_object()) {
        warn() << "Error: Could not parse /var/run/utmp";
        return 1;
    }

    auto now = time(nullptr);

    printf("\033[1m%-10s %-12s %-16s %-20s %-6s\033[0m\n",
        "USER", "TTY", "FROM", "LOGIN@", "IDLE");
    json.value().as_object().for_each_member([&](auto& tty, auto& value) {
        const JsonObject& entry = value.as_object();
        auto uid = entry.get("uid").to_u32();
        auto pid = entry.get("pid").to_i32();
        (void)pid;
        auto from = entry.get("from").to_string();
        auto login_at = entry.get("login_at").to_string();

        auto* pw = getpwuid(uid);
        String username;
        if (pw)
            username = pw->pw_name;
        else
            username = String::number(uid);

        StringBuilder builder;
        String idle_string = "n/a";
        struct stat st;
        if (stat(tty.characters(), &st) == 0) {
            auto idle_time = now - st.st_mtime;
            if (idle_time >= 0) {
                builder.appendf("%d sec", idle_time);
                idle_string = builder.to_string();
            }
        }

        printf("%-10s %-12s %-16s %-20s %-6s\n",
            username.characters(),
            tty.characters(),
            from.characters(),
            login_at.characters(),
            idle_string.characters());
    });
    return 0;
}
