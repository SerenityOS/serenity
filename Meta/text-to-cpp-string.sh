#!/bin/sh
# $1 name of the variable
# $2 input path

echo "extern const char $1[];"
echo -n "const char $1[] = R\"("
grep -v '^ *#' < "$2" | while IFS= read -r line; do
  echo "$line"
done
echo ")\";"

