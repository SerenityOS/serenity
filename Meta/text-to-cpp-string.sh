#!/bin/sh
# $1 name of the variable
# $2 input path

echo "extern const char $1[];"
printf "const char %s[] = R\"(" "$1"
grep -v '^ *#' < "$2" | while IFS= read -r line; do
  echo "$line"
done
echo ")\";"

