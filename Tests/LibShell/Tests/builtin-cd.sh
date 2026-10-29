#!/bin/Shell

source $(dirname "$0")/test-commons.inc

# Some systems may give a path containing symlinks, we need a fully resolved path.
TEST_DIR="$(realpath "$(mktemp -d)")"
pushd "$TEST_DIR"

mkdir physical
ln -s physical logical

cd logical
if not [ "$PWD" = "$TEST_DIR/logical" ] { fail "Incorrect logically resolved path" }

cd "$TEST_DIR"
cd -P logical
if not [ "$PWD" = "$TEST_DIR/physical" ] { fail "Incorrect physically resolved path" }

cd "$TEST_DIR"
cd -PL logical
if not [ "$PWD" = "$TEST_DIR/logical" ] { fail "Incorrect logically resolved path, positional options" }

cd "$TEST_DIR"
cd -PLP logical
if not [ "$PWD" = "$TEST_DIR/physical" ] { fail "Incorrect physically resolved path, positional options" }

popd
rm -r "$TEST_DIR"

echo PASS
