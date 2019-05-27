#!/bin/sh
PORT_DIR=SDL
function fetch() {
    run_fetch_git "https://github.com/SerenityOS/SDL"
}
function configure() {
    run_configure_cmake
}
function build() {
    run_make
}
function install() {
    run_make_install
}
source ./.port_include.sh
