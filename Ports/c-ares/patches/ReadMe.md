# Patches for c-ares on SerenityOS

## `0001-Include-arpa-inet.h.patch`

Include arpa/inet.h

This seems wrong as INET6_ADDRSTRLEN belongs in netinet/in.h according
to POSIX, but for now this is the easiest fix to get the port to build.

