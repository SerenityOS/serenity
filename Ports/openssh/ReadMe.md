# Known limitations
- No SK/FIDO support.
- No DNS support.
- No proxy support.
- Assumes SSH2.0 for now.
- Cannot determine compatibility flags.
  This means there may be some weird bugs when connecting to certain SSH implementations.
- SSHD does not work as it requires socketpair. It will start, but will crash on connection.
