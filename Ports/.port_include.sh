#!/bin/bash
set -eu

SCRIPT=`dirname $0`
export SERENITY_ROOT=`realpath $SCRIPT/../`
prefix=$SERENITY_ROOT/Ports

export CC=i686-pc-serenity-gcc
export CXX=i686-pc-serenity-g++
export PATH=$SERENITY_ROOT/Toolchain/Local/bin:$PATH

. "$@"
shift

: "${makeopts:=-j$(nproc)}"
: "${installopts:=}"
: "${workdir:=$port-$version}"
: "${configscript:=configure}"
: "${configopts:=}"
: "${useconfigure:=false}"
: "${depends:=}"
: "${patchlevel:=1}"

run_nocd() {
    echo "+ $@ (nocd)"
    ("$@")
}
run() {
    echo "+ $@"
    (cd "$workdir" && "$@")
}
run_replace_in_file(){
    run perl -p -i -e "$1" $2
}
# Checks if a function is defined. In this case, if the function is not defined in the port's script, then we will use our defaults. This way, ports don't need to include these functions every time, but they can override our defaults if needed.
func_defined() {
    PATH= command -V "$1"  > /dev/null 2>&1
}
func_defined fetch || fetch() {
    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename <<< $(echo "$f")
        if [ -f "$filename" ]; then
            echo "$filename already exists"
        else
            run_nocd curl ${curlopts:-} "$url" -o "$filename"
        fi
        case "$filename" in
            *.tar*|.tbz*|*.txz|*.tgz)
                run_nocd tar xf "$filename"
                ;;
            *.gz)
                run_nocd gunzip "$filename"
                ;;
            *.zip)
                run_nocd bsdtar xf "$filename" || run_nocd unzip -qo "$filename"
                ;;
            *)
                echo "Note: no case for file $filename."
                ;;
        esac
    done
    if [ -d patches ]; then
        for f in patches/*; do
            run patch -p"$patchlevel" < "$f"
        done
    fi
}
func_defined configure || configure() {
    run ./"$configscript" --host=i686-pc-serenity $configopts
}
func_defined build || build() {
    run make $makeopts
}
func_defined install || install() {
    run make DESTDIR="$SERENITY_ROOT"/Root $installopts install
}
func_defined clean || clean() {
    rm -rf "$workdir" *.out
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
    rm -rf "$workdir" *.out
    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename hash <<< $(echo "$f")
        rm -f "$filename"
    done
}
addtodb() {
    if [ ! -f "$prefix"/packages.db ]; then
        echo "Note: $prefix/packages.db does not exist. Creating."
        touch "$prefix"/packages.db
    fi
    if ! grep -E "^(auto|manual) $port $version" "$prefix"/packages.db > /dev/null; then
        echo "Adding $port $version to database of installed ports!"
        if [ "${1:-}" = "--auto" ]; then
            echo "auto $port $version" >> "$prefix"/packages.db
        else
            echo "manual $port $version" >> "$prefix"/packages.db
            if [ ! -z "${dependlist:-}" ]; then
                echo "dependency $port$dependlist" >> "$prefix/packages.db"
            fi
        fi
    else
        >&2 echo "Warning: $port $version already installed. Not adding to database of installed ports!"
    fi
}
installdepends() {
    for depend in $depends; do
        dependlist="${dependlist:-} $depend"
    done
    for depend in $depends; do
        if ! grep "$depend" "$prefix"/packages.db > /dev/null; then
            (cd "../$depend" && ./package.sh --auto)
        fi
    done
}
uninstall() {
    if grep "^manual $port " "$prefix"/packages.db > /dev/null; then
        if [ -f plist ]; then
            for f in `cat plist`; do
                case $f in
                    */)
                        run rmdir "$SERENITY_ROOT/Root/$f" || true
                        ;;
                    *)
                        run rm -rf "$SERENITY_ROOT/Root/$f"
                        ;;
                esac
            done
            # Without || true, mv will not be executed if you are uninstalling your only remaining port.
            grep -v "^manual $port " "$prefix"/packages.db > packages.dbtmp || true
            mv packages.dbtmp "$prefix"/packages.db
        else
            >&2 echo "Error: This port does not have a plist yet. Cannot uninstall."
        fi
    else
        >&2 echo "Error: $port is not installed. Cannot uninstall."
    fi
}
do_fetch() {
    installdepends
    echo "Fetching $port!"
    fetch
}
do_configure() {
    if [ "$useconfigure" = "true" ]; then
        echo "Configuring $port!"
        chmod +x "${workdir}"/"$configscript"
        configure
    else
        echo "This port does not use a configure script. Skipping configure step."
    fi
}
do_build() {
    echo "Building $port!"
    build
}
do_install() {
    echo "Installing $port!"
    install
    addtodb "${1:-}"
}
do_clean() {
    echo "Cleaning workdir and .out files in $port!"
    clean
}
do_clean_dist() {
    echo "Cleaning dist in $port!"
    clean_dist
}
do_clean_all() {
    echo "Cleaning all in $port!"
    clean_all
}
do_uninstall() {
    echo "Uninstalling $port!"
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
