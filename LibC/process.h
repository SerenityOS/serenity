#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

pid_t spawn(const char* path, const char** args, const char** envp);

__END_DECLS

