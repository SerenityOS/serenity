#!/usr/bin/env bash

# the file containing the commit message is passed as the first argument
commit_file="$1"
commit_message=$(cat "$commit_file")

error() {
  echo -e "\033[0;31m$1:\033[0m"
  echo "$commit_message"
  exit 1
}

# fail if the commit message contains windows style line breaks (carriage returns)
if grep -q -U $'\x0D' "$commit_file"; then
  error "Commit message contains CRLF line breaks (only unix-style LF linebreaks are allowed)"
fi

line_number=0
while read -r line; do
  # ignore comment lines
  [[ "$line" =~ ^#.* ]] && continue

  ((line_number += 1))
  line_length=${#line}

  category_pattern="^\S.*?: .+"
  if [[ $line_number -eq 1 ]] && (echo "$line" | grep -P -v -q "$category_pattern"); then
    error "Missing category in commit title (if this is a fix up of a previous commit, it should be squashed)"
  fi

  if [[ $line_number -eq 1 ]] && [[ "$line" =~ \.$ ]]; then
    error "Commit title ends in a period"
  fi

  if [[ $line_length -gt 72 ]]; then
    error "Commit message lines are too long (maximum allowed is 72 characters)"
  fi
done <"$commit_file"
exit 0
