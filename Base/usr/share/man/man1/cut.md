## Name

cut - remove sections from each line of files

## Synopsis

```**sh
$ cut option... [file...]
```

## Description

Print selected parts of lines from each FILE to standard output.

With no FILE, or when FILE is -, read standard input.

## Arguments

* `file`: File(s) to cut

## Options

* `-b` `--bytes=list`: Select only these bytes
* `-f` `--fields=list`: select only these fields; also print any line that contains no delimiter character
* `-d` `--delimiter=delim`: use `delim` instead of `tab` for field delimiter


## Examples

```sh
$ cat example.txt
245:789 4567    M:4540  Admin   01:10:1980
535:763 4987    M:3476  Sales   11:04:1978

# Display first and third fields from file example.txt
$ cut example.txt -f 1,3
245:789	M:4540
535:763	M:3476

# Display first and third fields using `:` as a delimiter 
$ cut example.txt -d ':' -f 1,3
245:4540	Admin	01
535:3476	Sales	11

# Display bytes at given position 
$ echo "serenity is cool" | cut -b 5
n

```
