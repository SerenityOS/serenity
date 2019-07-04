#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

struct passwd {
    char* pw_name;
    char* pw_passwd;
    uid_t pw_uid;
    gid_t pw_gid;
    char* pw_gecos;
    char* pw_dir;
    char* pw_shell;
};

struct passwd* getpwent();
void setpwent();
void endpwent();
struct passwd* getpwnam(const char* name);
struct passwd* getpwuid(uid_t);

__END_DECLS
