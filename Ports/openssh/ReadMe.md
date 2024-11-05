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
