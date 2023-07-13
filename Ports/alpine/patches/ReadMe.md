# Patches for alpine on SerenityOS

## `0001-Update-pith-Makefile.in-to-compile-helpers-for-build.patch`

Update pith/Makefile.in to compile helpers for build system, not target


## `0002-Add-serenity-as-OS-type-in-Alpine.patch`

Add serenity as OS type in Alpine.


## `0003-Add-Alpine-system-configuration-file-that-specifies-.patch`

Add Alpine system configuration file that specifies location of CA certificates.

The system-certs-file configuration option is provided to
SSL_CTX_load_verify_locations() as the CAfile argument (via
imap/src/osdep/unix/ssl_unix.c).

system-certs-file and CAfile are used (as opposed to
system-certs-path and CApath, used to specify a directory)
because certificate files within the CApath directory must follow a
specific naming convention, where the filename matches the CA subject
name hash value.

See man 3 SSL_CTX_set_default_verify_paths for details on how OpenSSL
looks up CA certificates.
https://www.openssl.org/docs/man3.0/man3/SSL_CTX_set_default_verify_paths.html

