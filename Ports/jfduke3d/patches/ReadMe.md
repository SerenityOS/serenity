# Patches for jfduke3d on SerenityOS

## `0001-compat.h-Recognize-endianness-of-SerenityOS-by-picki.patch`

[compat.h] Recognize endianness of SerenityOS by picking up endian.h

We have endian.h so let's pick that header.

## `0002-mmulti.c-ifdef-out-network-related-stuff-we-currentl.patch`

[mmulti.c] #ifdef out network-related stuff we currently

This patches out network-relatd stuff like IP_PKTINFO, IPV6_PKTINFO, and
IP_RECVDSTADDR which we currently do not support.

This is merely done to make the code compile, and since we do not try to
support any multiplayer option or other network-related stuff it should
not matter for the time being.
