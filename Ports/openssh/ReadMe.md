
Builds [openssh-portable](https://github.com/openssh/openssh-portable) for Serenity.

# Known limitations

-   No SK/FIDO support.
-   No DNS support.
-   No proxy support.
-   Assumes SSH2.0 for now.
-   Cannot determine compatibility flags.
    This means there may be some weird bugs when connecting to certain SSH implementations.

# Autostart SSHD

Add something like this to your sync-local.sh

```
cat <<EOF >> mnt/etc/SystemServer.ini

[SSHServer]
Executable=/usr/local/sbin/sshd
Arguments=-D
KeepAlive=true
SystemModes=text,graphical

[SSHServerGenKeys]
Executable=/usr/local/bin/ssh-keygen
Arguments=-A
KeepAlive=false
SystemModes=text,graphical
EOF
```

# Usage

Configuration files for the daemon live in `/etc/ssh` and generally work like on other operating systems.

By default, Serenity launches in qemu with `localhost:2222` mapped to `guest:22`, allowing for SSH access.
To ssh into the qemu guest:

```bash
ssh -p 2222 anon@localhost
```

If you want to run sshd on a different port, modify the scripts in `Meta/` or set `SERENITY_EXTRA_QEMU_ARGS`.
