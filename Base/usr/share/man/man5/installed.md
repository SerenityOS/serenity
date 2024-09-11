## Name

installed.db - Package database format

## Description

`/usr/Ports/installed.db` is a file keeping track of installed packages (or ports, respectively). It is a simple text-based format suitable for editing by the port system shell scripts.

Each line of installed.db consists of several space-separated fields. A line may also be empty and therefore ignored.

The first field specifies what kind of entry the line contains:

-   `auto` specifies a port that is installed automatically, i.e. as a dependency of another port.
-   `manual` specifies a port that was installed manually.
-   `dependency` does not specify a new port, but the dependencies of a port. The port also has a `manual` or `auto` line somewhere else in the file.

The second field always specifies the port's name.

Lines that specify an installed port (`manual`/`auto`) have a third field specifying the version. Both name and version may be found exactly like this in the package.sh script of the port, and in the list of available ports.

Lines that specify a port's dependencies (`dependency`) have any number of additional fields. Each field contains the name of another port that this port depends on. Note that while the port scripts never create an empty dependency list, this is still valid and simply means that the port depends on nothing else.

## Examples

```
manual libfftw3 3.3.10
auto libopus 1.3.1
auto libsamplerate 0.2.2
auto libogg 1.3.5
auto flac 1.4.2
dependency flac libogg
auto lame 3.100
auto libmpg123 1.29.3
auto libvorbis 1.3.7
dependency libvorbis libogg
auto sqlite 3410200
auto libsndfile 1.2.2
dependency libsndfile flac lame libmpg123 libogg libopus libvorbis sqlite
manual rubberband 3.3.0
dependency rubberband libfftw3 libopus libsamplerate libsndfile
auto x264 baee400fa9ced6f5481a728138fed6e867b0ff7f
```
