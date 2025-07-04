## Name

iffysign - Sign and verify

## Synopsis

```**sh
$ iffysign [--generate] [--sign] [--verify] [--pubkey-file FILE] [--pubkey PUBLIC_KEY] [--secret-key-file FILE] [--signature FILE] [--file FILE] [--force] [--untrusted-comment COMMENT] [--comment COMMENT]
```

## Description

### Introduction

`iffysign` is a dead simple tool for signing files and verifying these signatures, including trusted file comments. It is able to both verify the _integrity_ of files (that is, they haven’t been modified during transmission, e.g. while downloading them from the internet), as well as the _authenticity_ of the files (that is, they have been signed by the person holding a certain known key).

iffysign uses the [Ed25519](https://ed25519.cr.yp.to/) system, which is a state-of-the-art elliptic asymmetric signature system. iffysign has a secret key, which is used to create signatures, and a public key, which can be shared freely and is used to verify signatures created with the secret key. In comparison to similarly-strong schemes like RSA-4096, Ed25519 keys are very short. Therefore, iffysign public keys fit in 56 characters of Base64, allowing them to be transmitted via chat applications, social media posts, QR codes, NFC tags, and other low-data and/or text-only transmission mediums. This makes the keys easier to distribute via a multitude of mediums, decreasing the likelihood of an attacker intercepting and replacing a public key across all transmission paths.

iffysign is based on minisign (see below for history). The command-line options are almost completely compatible, mostly except for option defaults. The key and signature file formats are completely compatible, with two main exceptions: First, iffysign does not support password-encrypted secret keys and can only create and read unencrypted secret keys. Second, iffysign only supports the new pre-hashed signing scheme, using the identifier `ED` (capital D).

To sign a file, iffysign hashes the file’s contents using [BLAKE2b-512](https://www.blake2.net/), a modern hashing function at least as secure as SHA-3. The hashing provides the aforementioned integrity protection. The hash is then signed using the Ed25519 secret key, creating the _file signature_. Furthermore, the concatenation of the trusted comment and this signature are signed again, creating the _global signature_. This verifies the authenticity of the trusted comment _in combination with_ the file (it is not possible to transfer a trusted comment and the global signature onto another file signed with the same key). Both signatures are stored in Base64 on disk.

### Usage

iffysign provides three primary operations invoked with the `-G`, `-V`, and `-S` options: generate keys, verify signatures, and sign files, respectively. Files to be signed or verified are specified with the `-m` option. Signatures by default are named the same as the operand file, with the extension `.iffy` appended.

When signing, the `-c` and `-t` options can be used to change the default untrusted and trusted comments. The untrusted comments of newly generated key pairs cannot be modified, but freely edited in the files afterwards.

Public keys can be provided as files with the `-p` option, or as the Base64 key string directly on the command line with the `-P` option.

iffysign returns 0 on successful operation, including a valid signature. It returns 1 if the signature is invalid or some other issue occurred. It returns 2 on invalid CLI arguments.

## Options

-   `--help` Display help message and exit.
-   `--version` Print version.
-   `-G`, `--generate` Generate a new key pair.
-   `-S`, `--sign` Sign a file.
-   `-V`, `--verify` Verify that a file's signature is valid.
-   `-p FILE`, `--pubkey-file FILE` Path to the public key file, default `iffysign.pub`.
-   `-P PUBLIC_KEY`, `--pubkey PUBLIC_KEY` Public key as base64. This is the same as the second line of public key files.
-   `-s FILE`, `--secret-key-file FILE` Secret key file, default `~/.config/iffysign/iffysign.sec`.
-   `-x FILE`, `--signature FILE` Signature file, default `<file>.iffy`.
-   `-m FILE`, `--file FILE` File to sign or verify.
-   `-f`, `--force` Force overwrite files if they already exist. This option applies to all files iffysign can write (signatures and keys).
-   `-c COMMENT`, `--untrusted-comment COMMENT` **Untrusted** (not signed) comment to add when signing. **Do not use this option** unless you know what you’re doing. iffysign will emit a warning if you only use this option to avoid accidentally attaching unverifiable (and therefore mostly worthless) comments.
-   `-t COMMENT`, `--comment COMMENT` Trusted comment to add when signing.

## Files

iffysign deals in three kinds of files: signatures, public keys (and public key files), and secret keys. All three file formats are text-only and use [Base64](help://man/1/base64) to encode binary data. The [minisign website](https://jedisct1.github.io/minisign/) documents most parts of the file formats including how to create them.

For unencrypted secret keys, the `kdf_algorithm` is `\0\0` (two null bytes instead of `Sc`), and salt, opslimit and memlimit are zeroed out. The BLAKE2b-256 checksum of the secret key can be all zeroes, meaning it was not filled in and should not be verified.

All files provide untrusted comments in their first line, which are not signed and may be changed arbitrarily without invalidating signatures or keys. Untrusted comments are for end user information, such as to distinguish a large collection of public key files you have received from various sources. While useful, untrusted comments, as their name implies, are not signed or verified, and may not be relied upon for authoritative information about the keys or signatures. For instance, if the untrusted comment of a public key specifies the owner’s email address, this address’s authenticity cannot be verified and an attacker may arbitrarily change the email address without changing the public key itself. This is in contrast to protocols like OpenPGP, which provide verified proof of identity. The inability to verify any data associated with the public key, including the key ID, is by design; if you wish to sign and verify identity information with the keys, you can use trusted signature comments or identity file formats with associated signatures.

The trusted comment of a signature is verified in the global signature below it. It cannot be changed without the file failing verification.

All keys used by iffysign have randomly generated key IDs. Key IDs are not signed and may be changed arbitrarily. They are mostly a convenience feature to avoid accidentally verifying signatures with an incorrect key. Key IDs are inherited from `minisign` and `signify` mostly to provide compatibility.

The default secret key is `~/.config/iffysign/iffysign.sec`. This directory (`~/.config/iffysign`) is recommended for public and secret keys alike. The default public key is `iffysign.pub` (in the current directory).

## Examples

```sh
# Generate a new key pair in the default locations (iffysign.pub and ~/.config/iffysign/iffysign.sec)
$ minisign -G

# Sign the myfile.txt file, using your default secret key at ~/.config/iffysign/iffysign.sec
$ minisign -Sm myfile.txt
# Include a trusted comment into the signature, which will also be signed
$ minisign -Sm myfile.txt -t "This comment will be signed as well"

# Verify a file’s signature residing in myfile.txt.iffy using the base64 public key string
$ minisign -Vm myfile.txt -P <pubkey>
# Verify the file with a public key file instead
$ minisign -Vm myfile.txt -p signature.pub
```

## History

iffysign is based on [minisign(1)](https://jedisct1.github.io/minisign/) and provides a mostly compatible command-line interface as well as compatible key and signature file formats. Minisign and iffysign are based on and partially compatible with OpenBSD’s [signify(1)](https://man.openbsd.org/signify), which introduced the concept of “dead simple” signing and verification with modern cryptography. signify was written in 2014 by Ted Unangst to provide package signing for OpenBSD, and more rationale has been provided in a [transcribed talk of his](https://www.openbsd.org/papers/bsdcan-signify.html). Minisign’s major advancement over signify is the introduction of trusted (i.e. signed) comments, where signify only provides untrusted comments.

iffysign’s name was chosen like signify’s as a portmanteau of the terms “sign” and “verify”.

## See Also

-   [`pkg`(1)](help://man/1/pkg)
-   [`base64`(1)](help://man/1/base64)
-   [`checksum`(1)](help://man/1/checksum): to only verify the integrity of files, not their authenticity
