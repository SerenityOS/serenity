#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

mode_t umask(mode_t);
int chmod(const char* pathname, mode_t);
int mkdir(const char* pathname, mode_t);

__END_DECLS
