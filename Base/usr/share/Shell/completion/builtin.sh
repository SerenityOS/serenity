#!/bin/Shell

_complete_unalias() {
    shift 2
    argsparser_parse \
        --add-positional-argument names --help-string _ --value-name _ --short-name '' --min 0 --max 9999 \
        -- $*
    name=''
    if test ${length $names} -ne 0 {
        name="$names[-1]"
    }
    invariant="${length $name}"
    for $(alias | grep "^$name") {
        n=${regex_replace '"' '\"' ${regex_replace '\\([^\\])' '\1' ${regex_replace '=.*' '' "$it"}}}
        v=${regex_replace '"' '\"' ${regex_replace '\\([^\\])' '\1' ${regex_replace '[^=]*=' '' "$it"}}}
        echo '{"kind":"plain","completion":"'"$n"'", "trailing_trivia":" ", "display_trivia":"'"$v"'", "invariant_offset": '$invariant'}'
    }
}

__complete_job_spec() {
    match $1 as hint {
        %?* as (name) {
            for $(jobs | grep "$name") {
                id=''
                match $it {
                    [*]\ * as (i _) { id=$i }
                    * { continue }
                }
                echo '{"kind":"plain","static_offset":'"${length "?$name"}"',"invariant_offset":0,"completion":"'"$id"'"}'
            }
        }
        %* as (id) {
            invariant=${length $id}
            for $(jobs | grep "^\\[$id\\d+\\]") {
                id=''
                match $it {
                    [*]\ * as (i _) { id=$i }
                    * { continue }
                }
                echo '{"kind":"plain","static_offset":0,"invariant_offset":'"$invariant"',"completion":"'"$id"'"}'
            }
        }
        (?<pid>^\d+$) {
            invariant=${length $pid}
            for $(ps -e | grep "^ *$pid") {
                id=''
                description=''
                match $it {
                    "*$pid* *" as (_ i rest) { id="$pid$i" description="$rest" }
                    * { continue }
                }
                echo '{"kind":"plain","static_offset":0,"invariant_offset":'"$invariant"',"completion":"'"$id"'","display_trivia":"'"$description"'"}'
            }
        }
        * as (name) {
            static="${length $name}"
            for $(ps -e | grep "$name") {
                id=''
                description=''
                match $it {
                    (?: *(?<pid>\d+) (?<rest>.*)) { id="$pid" description="$rest" }
                    * { continue }
                }
                echo '{"kind":"plain","static_offset":'"$static"',"invariant_offset":0,"completion":"'"$id"'","display_trivia":"'"$description"'","allow_commit_without_listing":false}'
            }
        }
    }
}

_complete_kill() {
    if test $*[-1] = '--' {
        __complete_job_spec ''
    } else {
        __complete_job_spec $*[-1]
    }
}

_complete_cd() {
    if test $*[-1] = '--' {
        invariant_offset=0
        results=${concat_lists .*/ */}
    } else {
        invariant_offset=${length "$*[-1]"}
        results=$(glob "$*[-1]*/")
    }

    for $results {
        echo '{"kind":"plain","static_offset":0,"invariant_offset":'"$invariant_offset"',"completion":"'"${remove_suffix / $it}"'","trailing_trivia":"/"}'
    }
}
