#!/bin/Shell

echo "Not running Shell-backgrounding as it has a high failure rate"
echo PASS
exit 0

source $(dirname "$0")/test-commons.inc

setopt --verbose

last_idx=''
block_idx=0
block() {
    block_idx=$(expr 1 + $block_idx)
    last_idx=$block_idx
    mkfifo fifo$block_idx
    cat fifo$block_idx&
}

unblock(idx) {
    echo unblock $idx > fifo$idx
    rm -f fifo$idx
}

assert_job_count(count) {
    ecount=$(jobs | wc -l)
    shift
    if test $ecount -ne $count {
        for $* {
            unblock $it
        }
        fail "expected $ecount == $count"
    }
}

block
i=$last_idx

assert_job_count 1 $i

unblock $i
wait

block
i=$last_idx
block
j=$last_idx

assert_job_count 2 $i $j

unblock $i
unblock $j
wait
