## Running SerenityOS Tests

There are two classes of tests built during a Serenity build: host tests and target tests. Host tests run on the build
machine, and use [Lagom](../Meta/Lagom/README.md) to build Serenity userspace libraries for the host platform. Target
tests run on the Serenity machine, either emulated or bare metal.

### Running Host Tests

There are two ways to build host tests: from a full build, or from a Lagom-only build. The only difference is the CMake
command used to initailize the build directory.

For a full build, pass `-DBUILD_LAGOM=ON` to the CMake command.

```sh
mkdir Build
cd Build
cmake .. -GNinja -DBUILD_LAGOM=ON
```

For a Lagom-only build, pass the Lagom directory to CMake.

```sh
mkdir BuildLagom
cd BuildLagom
cmake ../Meta/Lagom -GNinja
```

In both cases, the tests can be run via ninja after doing a build:

```sh
ninja && ninja test
```

### Running Target Tests

Tests built for the Serenity target get installed either into `/usr/Tests` or `/bin`. `/usr/Tests` is preferred, but
some system tests are installed into `/bin` for historical reasons.

The easiest way to run all of the known tests in the system is to use the `run-tests-and-shutdown.sh` script that gets
installed into `/home/anon/tests`. When running in CI, the environment variable `$DO_SHUTDOWN_AFTER_TESTS` is set, which
will run `shutdown -n` after running all the tests.

For completeness, a basic on-target test run will need the Serenity image built and run via QEMU.

```sh
mkdir Build
cd Build
cmake .. -GNinja
ninja && ninja install && ninja image && ninja run
```

In the initial terminal, one can easily run the test runner script:

```
courage ~ $ ./tests/run-tests-and-shutdown.sh
=== Running Tests on SerenityOS ===
...
```

CI runs the tests in self-test mode, using the 'ci' run options and the TestRunner entry in /etc/SystemServer.ini to run
tests automatically on startup.

The system server entry looks as below:

```ini
[TestRunner@ttyS0]
Executable=/home/anon/tests/run-tests-and-shutdown.sh
StdIO=/dev/ttyS0
Environment=DO_SHUTDOWN_AFTER_TESTS=1 TERM=xterm PATH=/bin:/usr/bin:/usr/local/bin
User=anon
WorkingDirectory=/home/anon
BootModes=self-test
```

`/dev/ttyS0` is used as stdio because that serial port is connected when qemu is run with `-display none` and
`-nographic`, and output to it will show up in the stdout of the qemu window. Seperately, the CI run script redirects
the serial debug output to `./debug.log` so that both stdout of the tests and the dbgln from the kernel/tests can be
captured.

To run with CI's TestRunner system server entry, Serenity needs booted in self-test mode. Running the following shell
lines will boot Serenity in self-test mode, run tests, and exit.

```sh
export SERENITY_RUN=ci
export SERENITY_KERNEL_CMDLINE="boot_mode=self-test"
ninja run
```
