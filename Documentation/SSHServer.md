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

The SSH server looks for authorized keys in `/home/anon/.config/ssh/authorized_keys`.
The simplest way to do it is to add these lines to `sync-local.sh`:

```bash
mkdir -p mnt/home/anon/.config/ssh/
cat path/to/serenity_ed25519.pub >> mnt/home/anon/.config/ssh/authorized_keys
```
