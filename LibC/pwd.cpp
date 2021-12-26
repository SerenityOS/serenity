#include <pwd.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>
#include <AK/AKString.h>

extern "C" {

#define PWDB_STR_MAX_LEN 256

struct passwd_with_strings : public passwd {
    char name_buffer[PWDB_STR_MAX_LEN];
    char passwd_buffer[PWDB_STR_MAX_LEN];
    char gecos_buffer[PWDB_STR_MAX_LEN];
    char dir_buffer[PWDB_STR_MAX_LEN];
    char shell_buffer[PWDB_STR_MAX_LEN];
};

static FILE* __pwdb_stream = nullptr;
static unsigned __pwdb_line_number = 0;
static struct passwd_with_strings* __pwdb_entry = nullptr;

void setpwent()
{
    __pwdb_line_number = 0;
    if (__pwdb_stream) {
        rewind(__pwdb_stream);
    } else {
        __pwdb_stream = fopen("/etc/passwd", "r");
        if (!__pwdb_stream) {
            perror("open /etc/passwd");
        }
        assert(__pwdb_stream);
        __pwdb_entry = (struct passwd_with_strings*)mmap_with_name(nullptr, getpagesize(), PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, "setpwent");
    }
}

void endpwent()
{
    __pwdb_line_number = 0;
    if (__pwdb_stream) {
        fclose(__pwdb_stream);
        __pwdb_stream = nullptr;
    }
    if (__pwdb_entry) {
        munmap(__pwdb_entry, getpagesize());
        __pwdb_entry = nullptr;
    }
}

struct passwd* getpwuid(uid_t uid)
{
    setpwent();
    while (auto* pw = getpwent()) {
        if (pw->pw_uid == uid)
            return pw;
    }
    return nullptr;
}

struct passwd* getpwnam(const char* name)
{
    setpwent();
    while (auto* pw = getpwent()) {
        if (!strcmp(pw->pw_name, name))
            return pw;
    }
    return nullptr;
}

struct passwd* getpwent()
{
    if (!__pwdb_stream)
        setpwent();

    assert(__pwdb_stream);
    if (feof(__pwdb_stream))
        return nullptr;

next_entry:
    char buffer[1024];
    ++__pwdb_line_number;
    char* s = fgets(buffer, sizeof(buffer), __pwdb_stream);
    if (!s)
        return nullptr;
    assert(__pwdb_stream);
    if (feof(__pwdb_stream))
        return nullptr;
    String line(s, Chomp);
    auto parts = line.split(':');
    if (parts.size() != 7) {
        fprintf(stderr, "getpwent(): Malformed entry on line %u\n", __pwdb_line_number);
        goto next_entry;
    }
    auto& e_name = parts[0];
    auto& e_passwd = parts[1];
    auto& e_uid_string = parts[2];
    auto& e_gid_string = parts[3];
    auto& e_gecos = parts[4];
    auto& e_dir = parts[5];
    auto& e_shell = parts[6];
    bool ok;
    uid_t e_uid = e_uid_string.to_uint(ok);
    if (!ok) {
        fprintf(stderr, "getpwent(): Malformed UID on line %u\n", __pwdb_line_number);
        goto next_entry;
    }
    gid_t e_gid = e_gid_string.to_uint(ok);
    if (!ok) {
        fprintf(stderr, "getpwent(): Malformed GID on line %u\n", __pwdb_line_number);
        goto next_entry;
    }
    __pwdb_entry->pw_uid = e_uid;
    __pwdb_entry->pw_gid = e_gid;
    __pwdb_entry->pw_name = __pwdb_entry->name_buffer;
    __pwdb_entry->pw_passwd = __pwdb_entry->passwd_buffer;
    __pwdb_entry->pw_gecos = __pwdb_entry->gecos_buffer;
    __pwdb_entry->pw_dir = __pwdb_entry->dir_buffer;
    __pwdb_entry->pw_shell = __pwdb_entry->shell_buffer;
    strncpy(__pwdb_entry->name_buffer, e_name.characters(), PWDB_STR_MAX_LEN);
    strncpy(__pwdb_entry->passwd_buffer, e_passwd.characters(), PWDB_STR_MAX_LEN);
    strncpy(__pwdb_entry->gecos_buffer, e_gecos.characters(), PWDB_STR_MAX_LEN);
    strncpy(__pwdb_entry->dir_buffer, e_dir.characters(), PWDB_STR_MAX_LEN);
    strncpy(__pwdb_entry->shell_buffer, e_shell.characters(), PWDB_STR_MAX_LEN);
    return __pwdb_entry;
}

}
