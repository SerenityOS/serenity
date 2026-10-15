#!/bin/Shell

source $(dirname "$0")/test-commons.inc

rm -rf /tmp/shell-test 2> /dev/null
mkdir -p /tmp/shell-test
cd /tmp/shell-test

mkdir physical
ln -s physical logical

cd logical
if not [ "${PWD}" = "/tmp/shell-test/logical" ] { fail "Incorrect logically resolved path" }

cd /tmp/shell-test
cd -P logical
if not [ "${PWD}" = "/tmp/shell-test/physical" ] { fail "Incorrect physically resolved path" }

cd /tmp/shell-test
cd -PL logical
if not [ "${PWD}" = "/tmp/shell-test/logical" ] { fail "Incorrect logically resolved path, positional options" }

cd /tmp/shell-test
cd -PLP logical
if not [ "${PWD}" = "/tmp/shell-test/physical" ] { fail "Incorrect physically resolved path, positional options" }

rm -rf /tmp/shell-test

echo PASS
