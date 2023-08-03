## Name

cal - Display a calendar

## Synopsis

```**sh
$ cal [--starting-day weekday] [--three-month-view] [--year] [[month] year]
```

## Description

This program displays a simple calendar. If no arguments are specified, the current month is displayed with the current day highlighted.
An overview of a whole year is displayed when a `year` is passed without a `month`.

The current day is always highlighted.

Months and years are specified with numbers. Weeks start at what's configured in the Calendar system settings (Default is Sunday),
unless the `--starting-day` option is passed.

### Note on Calenders

This program only supports "Gregorian" calendar.

On "September 3, 1752" the Gregorian calendar was adopted by Britain, marking that day's date as "September 14, 1752."
So, (unlike this program's output) "September 13, 1752" actually should be "September 2, 1752" for historical correctness.

## Arguments

* `year`:  Year as a number (0 to 2147483647). The current year is the default value.
* `month`: Month as a number (1 to 12).

## Options

* `-s`, `--starting-day`: Specify which day should start the week. Accepts either short or long weekday names or indexes (0 being Sunday).
* `-3`, `--three-month-view`: Along with the month specified (by the `month` argument), display previous and next month as well.
* `-y`, `--year`: Display the current year by laying out months on a grid. Short-hand for `cal $current_year`.

## Examples

```sh
# Display the current month (August 2023)
$ cal
   August - 2023
Su Mo Tu We Th Fr Sa
       1  2  3  4  5
 6  7  8  9 10 11 12
13 14 15 16 17 18 19
20 21 22 23 24 25 26
27 28 29 30 31

# Display any month
$ cal 1 1999
   January - 1999
Su Mo Tu We Th Fr Sa
                1  2
 3  4  5  6  7  8  9
10 11 12 13 14 15 16
17 18 19 20 21 22 23
24 25 26 27 28 29 30
31

# Display three months side-by-side
$ cal -3 12 2018
  November - 2018       December - 2018        January - 2019
Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
             1  2  3                     1         1  2  3  4  5
 4  5  6  7  8  9 10   2  3  4  5  6  7  8   6  7  8  9 10 11 12
11 12 13 14 15 16 17   9 10 11 12 13 14 15  13 14 15 16 17 18 19
18 19 20 21 22 23 24  16 17 18 19 20 21 22  20 21 22 23 24 25 26
25 26 27 28 29 30     23 24 25 26 27 28 29  27 28 29 30 31
                      30 31

# Display an entire year
$ cal 2023
                           Year 2023


      January               February               March
Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
 1  2  3  4  5  6  7            1  2  3  4            1  2  3  4
 8  9 10 11 12 13 14   5  6  7  8  9 10 11   5  6  7  8  9 10 11
15 16 17 18 19 20 21  12 13 14 15 16 17 18  12 13 14 15 16 17 18
22 23 24 25 26 27 28  19 20 21 22 23 24 25  19 20 21 22 23 24 25
29 30 31              26 27 28              26 27 28 29 30 31


       April                  May                   June
Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
                   1      1  2  3  4  5  6               1  2  3
 2  3  4  5  6  7  8   7  8  9 10 11 12 13   4  5  6  7  8  9 10
 9 10 11 12 13 14 15  14 15 16 17 18 19 20  11 12 13 14 15 16 17
16 17 18 19 20 21 22  21 22 23 24 25 26 27  18 19 20 21 22 23 24
23 24 25 26 27 28 29  28 29 30 31           25 26 27 28 29 30
30


        July                 August              September
Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
                   1         1  2  3  4  5                  1  2
 2  3  4  5  6  7  8   6  7  8  9 10 11 12   3  4  5  6  7  8  9
 9 10 11 12 13 14 15  13 14 15 16 17 18 19  10 11 12 13 14 15 16
16 17 18 19 20 21 22  20 21 22 23 24 25 26  17 18 19 20 21 22 23
23 24 25 26 27 28 29  27 28 29 30 31        24 25 26 27 28 29 30
30 31


      October               November              December
Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
 1  2  3  4  5  6  7            1  2  3  4                  1  2
 8  9 10 11 12 13 14   5  6  7  8  9 10 11   3  4  5  6  7  8  9
15 16 17 18 19 20 21  12 13 14 15 16 17 18  10 11 12 13 14 15 16
22 23 24 25 26 27 28  19 20 21 22 23 24 25  17 18 19 20 21 22 23
29 30 31              26 27 28 29 30        24 25 26 27 28 29 30

# Start the week with Friday
# same as: `cal -s Fr`, `cal -s FRI`,  and `cal -s 5` (weekday names are case insensitive)
$ cal -s friday
   August - 2023
Fr Sa Su Mo Tu We Th
             1  2  3
 4  5  6  7  8  9 10
11 12 13 14 15 16 17
18 19 20 21 22 23 24
25 26 27 28 29 30 31
```

## Errors and return values

```sh
# Incorrect usage of `-3` without specifying a `month`. Returns 1.
# same error for `cal -3 -y`
$ cal -3 1969
cal: Cannot use --three-month-view when displaying a year.
```
