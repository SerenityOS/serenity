# Ports management architecture

# Filesystem & archive layouts

### Archived package file

- `Root` - a directory that contains all files that should be installed
  with the package
- `details` - a file that contains all the dependencies that are required
  by the package.
- `files` - a file that contains all tracked files by a package

### Installed port directory

Each installed port has a directory under `/usr/Ports` with its name (which contains the
installed version).

For example, Installing `sed` in version 4.9 will create a directory called `sed-4.9`
which contains a file called `files`.
The `files` file contains all tracked files by a package. For `sed-4.9` it looks like this:
```
d ./usr
d ./usr/local
d ./usr/local/bin
f ./usr/local/bin/sed
d ./usr/local/share
d ./usr/local/share/info
f ./usr/local/share/info/sed.info
f ./usr/local/share/info/dir
d ./usr/local/share/man
d ./usr/local/share/man/man1
f ./usr/local/share/man/man1/sed.1
```

In addition to that, a file called `dependents` contains all programs that require
the port to be functional.

## The package manager

The package manager is considered the up-coming standard solution to handle
ports.

The features of package manager are the ability to:
- Install a port from a package
- Resolve port dependencies efficiently
- Remove installed ports
- Query information about available and installed ports

## The build scripts system

The build scripts system is a more raw solution than the standard package
manager.
For example, it doesn't provide an option to remove ports at all.
However, it can:
- Pack a port as a package for usage on multiple machines
- Install a port directly, as part of a development cycle

