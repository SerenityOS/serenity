#include <grp.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>
#include <AK/String.h>

extern "C" {

#define GRDB_STR_MAX_LEN 256

struct group_with_strings : public group {
    char name_buffer[GRDB_STR_MAX_LEN];
    char passwd_buffer[GRDB_STR_MAX_LEN];
    char* members[32];
    char members_buffer[32][32];
};

static FILE* __grdb_stream = nullptr;
static unsigned __grdb_line_number = 0;
static struct group_with_strings* __grdb_entry = nullptr;

void setgrent()
{
    __grdb_line_number = 0;
    if (__grdb_stream) {
        rewind(__grdb_stream);
    } else {
        __grdb_stream = fopen("/etc/group", "r");
        if (!__grdb_stream) {
            perror("open /etc/group");
        }
        assert(__grdb_stream);
        __grdb_entry = (struct group_with_strings*)mmap(nullptr, getpagesize());
        set_mmap_name(__grdb_entry, getpagesize(), "setgrent");
    }
}

void endgrent()
{
    __grdb_line_number = 0;
    if (__grdb_stream) {
        fclose(__grdb_stream);
        __grdb_stream = nullptr;
    }
    if (__grdb_entry) {
        munmap(__grdb_entry, getpagesize());
        __grdb_entry = nullptr;
    }
}

struct group* getgrgid(gid_t gid)
{
    setgrent();
    while (auto* gr = getgrent()) {
        if (gr->gr_gid == gid)
            return gr;
    }
    return nullptr;
}

struct group* getgrname(const char* name)
{
    setgrent();
    while (auto* gr = getgrent()) {
        if (!strcmp(gr->gr_name, name))
            return gr;
    }
    return nullptr;
}

struct group* getgrent()
{
    if (!__grdb_stream)
        setgrent();

    assert(__grdb_stream);
    if (feof(__grdb_stream))
        return nullptr;

next_entry:
    char buffer[1024];
    ++__grdb_line_number;
    char* s = fgets(buffer, sizeof(buffer), __grdb_stream);
    if (!s)
        return nullptr;
    assert(__grdb_stream);
    if (feof(__grdb_stream))
        return nullptr;
    String line(s, Chomp);
    auto parts = line.split(':');
    if (parts.size() != 4) {
        fprintf(stderr, "getgrent(): Malformed entry on line %u: '%s' has %u parts\n", __grdb_line_number, line.characters(), parts.size());
        goto next_entry;
    }
    auto& e_name = parts[0];
    auto& e_passwd = parts[1];
    auto& e_gid_string = parts[2];
    auto& e_members_string = parts[3];
    bool ok;
    gid_t e_gid = e_gid_string.toUInt(ok);
    if (!ok) {
        fprintf(stderr, "getgrent(): Malformed GID on line %u\n", __grdb_line_number);
        goto next_entry;
    }
    auto members = e_members_string.split(',');
    __grdb_entry->gr_gid = e_gid;
    __grdb_entry->gr_name = __grdb_entry->name_buffer;
    __grdb_entry->gr_passwd = __grdb_entry->passwd_buffer;
    for (size_t i = 0; i < members.size(); ++i) {
        __grdb_entry->members[i] = __grdb_entry->members_buffer[i];
        strcpy(__grdb_entry->members_buffer[i], members[i].characters());
    }
    __grdb_entry->members[members.size()] = nullptr;
    __grdb_entry->gr_mem = __grdb_entry->members;
    strncpy(__grdb_entry->name_buffer, e_name.characters(), GRDB_STR_MAX_LEN);
    strncpy(__grdb_entry->passwd_buffer, e_passwd.characters(), GRDB_STR_MAX_LEN);
    return __grdb_entry;
}

}
