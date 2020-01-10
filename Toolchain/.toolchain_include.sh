#!/usr/bin/env bash
set -eu

SCRIPT=`dirname $0`

export SERENITY_ROOT=`realpath $SCRIPT/../`
export SCRIPT_DIR=`realpath $SCRIPT`
export PREFIX=$SERENITY_ROOT/Toolchain/Local
export SYSROOT=$SERENITY_ROOT/Root
export ARCH=${ARCH:-"i686"}
export TARGET="$ARCH-pc-serenity"

. "$@"
shift

echo ARCH is "$ARCH"
echo PREFIX is "$PREFIX"
echo SYSROOT is "$SYSROOT"


MAKE=make
MD5SUM=md5sum
NPROC=nproc

if [ `uname -s` = "OpenBSD" ]; then
    MAKE=gmake
    MD5SUM="md5 -q"
    NPROC="sysctl -n hw.ncpuonline"
    export CC=egcc
    export CXX=eg++
    export with_gmp=/usr/local
    export LDFLAGS="$LDFLAGS -Wl,-z,notext"
fi

export PARALLEL_BUILD="-j$($NPROC)"

: "${makeopts:=}"
: "${installopts:=install}"
: "${sourcedir:=$package-$version}"
: "${builddir:=$sourcedir/build}"
: "${configscript:=configure}"
: "${configopts:=}"
: "${useconfigure:=false}"
: "${depends:=}"
: "${patchlevel:=1}"

run_nocd() {
    echo "+ $@ (nocd)"
    ("$@")
}
run_sourcedir() {
    echo "+ $@"
    (cd "$sourcedir" && "$@")
}
run_builddir() {
    echo "+ $@"
    (cd "$builddir" && "$@")
}
run_replace_in_file(){
    run_sourcedir perl -p -i -e "$1" $2
}
# Checks if a function is defined. In this case, if the function is not defined in the packages's script, then we will use our defaults. This way, packages don't need to include these functions every time, but they can override our defaults if needed.
func_defined() {
    PATH= command -V "$1"  > /dev/null 2>&1
}

func_defined postfetch || postfetch() {
    echo ""
}
func_defined postinstall || postinstall() {
    echo ""
}

func_defined fetch || fetch() {
    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename expected_md5sum <<< $(echo "$f")
        
        # download
        if [ -f "$filename" ]; then
            echo "$filename already exists"
        else
            run_nocd curl ${curlopts:-} "$url" -o "$filename"
        fi

        # check md5sum if given
        echo "Expecting md5sum: $expected_md5sum"
        if [ ! -z "$expected_md5sum" ]; then
            local_md5sum="$($MD5SUM $filename | cut -f1 -d' ')"
            echo "md5sum($filename) ='$local_md5sum'"
            if [ "$local_md5sum" != "$expected_md5sum" ]; then
                # remove downloaded file to re-download on next run
                rm -f $filename 
                echo "md5sum's mismatching, removed erronous download. Please run script again."
                exit 1
            fi
        fi

        # extract
        if [ ! -f "$sourcedir"/.was_extracted ]; then
            case "$filename" in
                *.tar*|.tbz*|*.txz|*.tgz)
                    run_nocd tar xf "$filename"
                    run_sourcedir touch .was_extracted
                    ;;
                *.gz)
                    run_nocd gunzip "$filename"
                    run_sourcedir touch .was_extracted
                    ;;
                *.zip)
                    run_nocd bsdtar xf "$filename" || run_nocd unzip -qo "$filename"
                    run_sourcedir touch .was_extracted
                    ;;
                *)
                    echo "Note: no case for file $filename."
                    ;;
            esac
        fi
    done

    # patch if it was not yet patched (applying patches multiple times doesn't work!)
    if [ -d patches ] && [ ! -f "$sourcedir"/.was_patched ]; then
        for f in patches/*; do
            run_sourcedir patch -p"$patchlevel" < "$f"
        done
        run_sourcedir touch .was_patched
    fi

    postfetch
}
func_defined configure || configure() {
    mkdir -p "${builddir}"
    rel=$(realpath --relative-to $builddir $sourcedir)
    run_builddir "${rel}/${configscript}" --prefix=$PREFIX $configopts
}
func_defined build || build() {
    mkdir -p "${builddir}"
    run_builddir $MAKE $PARALLEL_BUILD $makeopts
}
func_defined install || install() {
    mkdir -p "${builddir}"
    run_builddir $MAKE $PARALLEL_BUILD $installopts
    postinstall
}
func_defined clean || clean() {
    rm -rf "$builddir" *.out
}
func_defined clean_dist || clean_dist() {
    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename hash <<< $(echo "$f")
        rm -f "$filename"
    done
}
func_defined clean_all || clean_all() {
    rm -rf "$sourcedir" "$builddir" *.out
    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename hash <<< $(echo "$f")
        rm -f "$filename"
    done
}
addfile() {
    # expecting parameters
    # $1 base url
    # $2 filename
    # $3 md5sum
    files="$files\n$1/$2 $2 $3"
}
export addfile

addtodb() {
    if [ ! -f "$SCRIPT_DIR"/packages.db ]; then
        echo "Note: $SCRIPT_DIR/packages.db does not exist. Creating."
        touch "$SCRIPT_DIR"/packages.db
    fi
    if ! grep -E "^(auto|manual) $package $version" "$SCRIPT_DIR"/packages.db > /dev/null; then
        echo "Adding $package $version to database of installed toolchain packages!"
        if [ "${1:-}" = "--auto" ]; then
            echo "auto $package $version" >> "$SCRIPT_DIR"/packages.db
        else
            echo "manual $package $version" >> "$SCRIPT_DIR"/packages.db
            if [ ! -z "${dependlist:-}" ]; then
                echo "dependency $package$dependlist" >> "$SCRIPT_DIR/packages.db"
            fi
        fi
    else
        >&2 echo "Warning: $package $version already installed. Not adding to database of installed toolchain packages!"
    fi
}
installdepends() {
    for depend in $depends; do
        dependlist="${dependlist:-} $depend"
    done
    for depend in $depends; do
        # Instead of skipping already built dependencies, they shall be built every time!
        if ! grep "$depend" "$SCRIPT_DIR"/packages.db > /dev/null; then
            echo "Rebuild already built dependency $depend"
        fi
        (cd "../$depend" && ./package.sh --auto)
    done
}
uninstall() {
    >&2 echo "Error: uninstall not implemented."
}
do_fetch() {
    installdepends
    echo "Fetching $package!"
    fetch
}
do_configure() {
    if [ "$useconfigure" = "true" ]; then
        echo "Configuring $package!"
        chmod +x "${sourcedir}/${configscript}"
        configure
    else
        echo "This package does not use a configure script. Skipping configure step."
    fi
}
do_build() {
    echo "Building $package!"
    build
}
do_install() {
    echo "Installing $package!"
    install
    addtodb "${1:-}"
}
do_clean() {
    echo "Cleaning builddir and .out files in $package!"
    clean
}
do_clean_dist() {
    echo "Cleaning dist in $package!"
    clean_dist
}
do_clean_all() {
    echo "Cleaning all in $package!"
    clean_all
}
do_uninstall() {
    echo "Uninstalling $package!"
    uninstall
}
do_all() {
    do_fetch
    do_configure
    do_build
    do_install "${1:-}"
}

if [ -z "${1:-}" ]; then
    do_all
else
    case "$1" in
        fetch|configure|build|install|clean|clean_dist|clean_all|uninstall)
            do_$1
            ;;
        --auto)
            do_all $1
            ;;
        *)
            >&2 echo "I don't understand $1! Supported arguments: fetch, configure, build, install, clean, clean_dist, clean_all, uninstall."
            exit 1
            ;;
    esac
fi
