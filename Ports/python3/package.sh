#!/usr/bin/env -S bash ../.port_include.sh

source version.sh

port=python3
version="${PYTHON_VERSION}"
workdir="Python-${version}"
useconfigure="true"
files="${PYTHON_ARCHIVE_URL} ${PYTHON_ARCHIVE}
https://www.python.org/ftp/python/${version}/Python-${version}.tar.xz.asc Python-${version}.tar.xz.asc
https://raw.githubusercontent.com/python/cpython/942dd9f3f77eef08fabddbd9fb883a866ad6d4cb/PC/pycon.ico launcher.ico"
auth_type="sig"
auth_import_key="E3FF2839C048B25C084DEBE9B26995E310250568"
auth_opts="Python-${version}.tar.xz.asc Python-${version}.tar.xz"
launcher_name="Python"
launcher_category="Development"
launcher_command="/usr/local/bin/python3"
launcher_run_in_terminal="true"
icon_file="../launcher.ico" # This is an older icon that's downloaded separately, so we need to go outside of $workdir

# We could also add `openssl` here, but the _ssl modules doesn't build at the moment 
depends="bzip2 libffi ncurses readline sqlite termcap zlib"

# FIXME: --enable-optimizations results in lots of __gcov_* linker errors
configopts="--disable-ipv6 --without-ensurepip ac_cv_file__dev_ptmx=no ac_cv_file__dev_ptc=no"

export CC="${CC} --sysroot=${SERENITY_INSTALL_ROOT}"
export BLDSHARED="${CC} -shared"

pre_configure() {
    build="$("${workdir}/config.guess")"  # e.g. 'x86_64-pc-linux-gnu'
    configopts="${configopts} --build=${build}"
}

# Note: The showproperty command is used when linting ports, we don't actually need python at this time.
if [ "$1" != "showproperty" ]; then
    if [ -x "$(command -v python3)" ]; then
        # Check if major and minor version of python3 are matching
        if ! python3 -c "import sys; major, minor, _ = map(int, '${PYTHON_VERSION}'.split('.')); sys.exit(not (sys.version_info.major == major and sys.version_info.minor == minor))"; then
            echo "Error: python3 version does not match needed version to build ${PYTHON_VERSION}" >&2
            echo "Build this Python version on your host using Toolchain/BuildPython.sh or install it otherwise and try again." >&2
            exit 1
        fi
    else
        echo "Error: python3 is not installed but is required to build ${PYTHON_VERSION}" >&2
        echo "Build this Python version on your host using Toolchain/BuildPython.sh or install it otherwise and try again." >&2
        exit 1
    fi
fi
