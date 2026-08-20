# Connecting to Serenity's SSH Server

Serenity's SSH server currently supports only a subset of standard SSH features.

## Authentication

The server only supports the `publickey` authentication method.

-   Your **client** must have a private key.
-   The **server** must have access to the corresponding public key.

> NOTE: Only **ED25519** keys are supported. Other key types (RSA, ECDSA, etc.) will not work.

### Generating a Key Pair

To generate a compatible key pair, run the following command on your host machine:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/serenity_ed25519
```

### Installing the public key in SerenityOS

To allow SSH access, you need to install your public key into the SerenityOS image so the SSH server can authenticate your client.

The SSH server looks for authorized keys in `$HOME/.config/ssh/authorized_keys`.
The simplest way to do it is to add these lines to `sync-local.sh`:

```bash
mkdir -p mnt/home/anon/.config/ssh/
cat path/to/serenity_ed25519.pub >> mnt/home/anon/.config/ssh/authorized_keys
```

## Server Identity

Every SSH server has its own identity, represented by a host key pair.
If this identity changes between two connections, clients will detect it and abort as this is one of the symptoms of a man-in-the-middle attack.

SSHServer only supports generating ephemeral keys, so this will cause clients to warn about the server's changed identity every time the server is restarted.
To fix this issue, we can manually generate a key and provide it to the server.

### Generating a Key Pair

The steps are similar to those in the section above, first generate a key pair.
This key doesn't have to live in your `.ssh` directory, so I recommend to put it somewhere else.
As an example:

```bash
ssh-keygen -t ed25519 -f path/to/serenity/ssh_keys/host_ed25519
```

### Installing the key pair in SerenityOS

Then add this to `sync-local.sh` to copy the key pair to SerenityOS's image.

```bash
mkdir -p mnt/etc/ssh/
cp path/to/serenity/ssh_keys/host_ed25519* mnt/etc/ssh/
chown root:root mnt/etc/ssh/host_ed25519*
chmod u=r,g=,o= mnt/etc/ssh/host_ed25519*
```
