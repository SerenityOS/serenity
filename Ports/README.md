# Ports for SerenityOS

## What's this?

Serenity has software patched to run on it.
These shell scripts will allow you to build that sort of software, easily.
Note that you must have already built Serenity, and be in a Serenity build
environment.

## Available ports

A list of all available ports can be found [here](AvailablePorts.md).

## Using ports scripts

Each port has a script called `package.sh` which defines a name and version,
its dependencies, the required files that will be downloaded as well as
configuration/compilation options, and some other things (see
[Writing ports scripts](#writing-ports-scripts) for details).

- To install a certain port, `cd` into its directory and run `./package.sh`
- To install all available ports, run the `build_all.sh` script in this
  directory. Pass `clean` as first argument to remove old build files
  beforehand.
- To reinstall all currently installed ports, run the `build_installed.sh`
  script in this directory. This is sometimes required when LibC changes, for
  example. Pass `clean` as first argument to remove old build files beforehand.

Installed ports are being tracked in `Build/i686/Root/usr/Ports/packages.db` (a simple text file).
You can delete this file at any time, in fact it must be edited or removed
when clearing the build directory as port dependencies may not be installed
again otherwise.

Not giving an option is equivalent to `installdepends`, `fetch`, `patch`,
`configure`, `build` and `install`, in that order. This is recommended for a
regular install.

### Options

The following options are available:

#### `fetch`

By default, download, verify, and extract the port's [`files`](#files).

#### `patch`

Apply the port's patches (`patches/*.patch`). A file `.foo_applied` is created
in [`workdir`](#workdir) upon success to ensure a certain patch is only
applied once.

#### `configure`

By default, run the port's [`configscript`](#configscript) (usually
`configure`) with [`configopts`](#configopts).

#### `build`

By default, run `make` with the port's [`makeopts`](#makeopts).

#### `install`

By default, run `make install` with the port's [`installopts`](#installopts).

#### `shell`

Open a shell in the `$workdir` with the build environment set.

#### `installdepends`

Install all ports from the port's [`depends`](#depends) list.

#### `clean`

By default, remove all `.out` files from the port's [`workdir`](#workdir).

#### `clean_dist`

By default, remove everything that's been downloaded from the port's
[`files`](#files) list.

#### `clean_all`

By default, [`clean`](#clean) and [`clean_dist`](#clean_dist) combined.

#### `uninstall`

Remove the port's files from the Serenity build directory, if it has a `plist`
file.

#### `--auto`

Same as no option, but mark the port as having been installed automatically.
This is used for dependencies.

## Writing ports scripts

The `package.sh` file is a simple Bash script that's required for each port.
Patches and other files are optional. The most basic version of such a port
script simply defines some well-known variables and looks like this:

```bash
#!/usr/bin/env -S bash ../.port_include.sh

port="foo"
version="1.2.3"
useconfigure="true"
files="https://example.com/foo-${version}.tar.gz foo-${version}.tar.gz"
depends="bar baz"
```

The script in the shebang, [`.port_include.sh`](./.port_include.sh), is where
all the magic happens.

### Variables

The following variables have special functionality:

#### `auth_import_key`

PGP key to import (from `keyserver.ubuntu.com`) when [`auth_type`](#auth_type)
is `sig`.

#### `auth_opts`

Options passed to `gpg --verify` when [`auth_type`](#auth_type) is `sig`.

Usually used like this:

```bash
auth_opts="foo-${version}.tar.xz.asc foo-${version}.tar.xz"
```

#### `auth_type`

The type of file validation to use, can be one of:

- `md5`: Use MD5 hashes defined in [`files`](#files)
- `sha256`: Use SHA256 hashes defined in [`files`](#files)
- `sha1`: Use SHA1 hashes defined in [`files`](#files)
- `sig`: Use PGP signatures (see [`auth_opts`](#auth_opts))

Defaults to `md5`, most ports use `sig` though as `.asc` files are widely
available.

#### `configopts`

Options passed to the port's [`configscript`](#configscript) in the default
`configure` function.

`--host=i686-pc-serenity` is always passed, override the `configure` function
if that's undesirable.

#### `configscript`

Name of the script that will be run in the default `configure` function when
[`useconfigure`](#useconfigure) is `true.`

Defaults to `configure`.

#### `depends`

A space-separated list of other SerenityOS ports the port depends on and which
will be installed during the `installdepends` step.

#### `files`

A list of external files required by the port, one per line. The format of each
line is as follows:

```text
URL NAME HASH
```

Where `URL` is the URL from where the file will be downloaded (using `curl`),
`NAME` is the output name of the downloaded file, and `HASH` is an optional
MD5, SHA1, or SHA256 hash that will be used for verification when
[`auth_type`](#auth_type) is set to either of those hash functions.

For example:

```bash
files="https://example.com/foo-${version}.tar.xz foo-${version}.tar.xz
https://example.com/foo-${version}.tar.xz.asc foo-${version}.tar.xz.asc"
```

If a file is a compressed tar archive, a gzip compressed file or a zip
compressed file, it will be extracted.

If a file is an `.asc` file (PGP signature) it will be imported into `gpg`'s
keyring and can later be used for verification using [`auth_opts`](#auth_opts).

#### `icon_file`

The file to use for the port launcher icon. The icon file is assumed to have a
16x16 as well as a 32x32 layer.

#### `installopts`

Options passed to `make install` in the default `install` function.

`DESTDIR="${SERENITY_INSTALL_ROOT}"` (`"${SERENITY_SOURCE_DIR}/Build/${SERENITY_ARCH}/Root"`)
is always passed, override the `install` function if that's undesirable.

#### `makeopts`

Options passed to `make` in the default `build` function.

Defaults to `-j$(nproc)`.

#### `patchlevel`

The value for `patch`'s `-p` / `--strip` option, see `man patch` for details.

Defaults to `1`.

#### `port`

The "package name" of the port, usually the same as the directory this script
is placed in.

#### `prefix`

The location of the ports directory, only used for the `package.db` file for
now. Don't override this in ports contributed to Serenity.

Defaults to `$SERENITY_SOURCE_DIR/Ports`.

#### `useconfigure`

The `configure` step will run `pre_configure` and `configure` when this is set
to `true`, and simply skip them otherwise.

Defaults to `false`.

#### `version`

The version of the port. Written to `package.db`, and usually used with
variable interpolation in [`files`](#files) where the version is part of the
filename.

#### `workdir`

The working directory used for executing other commands via `run` as well as
cleanup. Usually the directory name of the upacked source archive.

Defaults to `$port-$version`.

### Functions

The various steps of the port installation process are split into individual
Bash functions, some of which can be overridden to provide custom behaviour,
like this:

```bash
build() {
    run mybuildtool --foo --bar
}
```

The following can be overridden, the names should be self-explanatory as they
mostly match the [available options](#options):

- `pre_fetch`
- `post_fetch`
- `pre_configure`
- `configure`.
- `build`
- `install`
- `post_install`
- `clean`
- `clean_dist`
- `clean_all`

A few (non-overridable) util functions are available as well:

#### `run`

Log the command and run it in the port's [`workdir`](#workdir).

#### `run_nocd`

Log the command and run it in the current working directory
(i.e. `Ports/$port`).

#### `run_replace_in_file`

Replace something in a file (using a Perl regular expression), like this:

```bash
run_replace_in_file "s/define FOO 1/undef FOO/" config.h
```

## How do I contribute?

You can either:

- Add new ports - just get the software to build and add the necessary patches
  and `package.sh` script
- Update an existing port: bumping its version, getting functionality to work
  that wasn't available so far etc. Make sure to update the patches
  accordingly.

Some videos of Andreas adding new ports can be found on YouTube, they might
help you understand how this usually works:

- [OS hacking: Porting the Bash shell](https://www.youtube.com/watch?v=QNK8vK-nkkg)
  (2019-05-20)
- [OS hacking: Porting DOOM to Serenity](https://www.youtube.com/watch?v=a0P_bB6wjhY)
  (2019-09-09)
- [OS hacking: Let's port git to SerenityOS!](https://www.youtube.com/watch?v=1-7VQwWo2Tg)
  (2020-02-19)
- [OS hacking: Fixing a resize bug with the vim port](https://www.youtube.com/watch?v=d4uVnawzHdQ)
  (2020-06-03)
