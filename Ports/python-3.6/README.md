# Python 3.6 Port

This port is highly experimental. Python binary can be started with `python3`, but many
functionality is expected to not work.

## Why this version is used

Python 2.7 will not be supported in future, see e.g. [pythonclock.org]([https://link](https://pythonclock.org/)).
Python 3 is a good candidate for porting. Until Python 3.6 it is easily possible to disable
multi-threading API via `--without-threads` option. This is needed until SerenityOS provides the
pthread APIs.

## How to improve

Run the Python test suite via `python3 -m test` to see what fails and start working on that.
If functionality in LibC/LibM/Kernel/... is updated, recompile Python with `./package.sh build`.

## Known limitations

* No locale support, default locale encoding set to utf-8

* Instead of `/dev/urandom`, `/dev/random` is being used

* No multi-threading

* time module not working due to missing time related functions in LibC/Kernel
