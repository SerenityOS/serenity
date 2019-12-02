## Name

cal - Display a calendar

## Synopsis

```**sh
$ cal [[[day] month] year]
```

## Description

This program displays a simple calendar. If no arguments are specified, the current month is displayed with the current day highlighted.

Days, months and years are specified with numbers. Week starts at Sunday.

## Examples

```sh
# Display the current month
$ cal
    12 - 1992    
Su Mo Tu We Th Fr Sa
             1  2  3
 4  5  6  7  8  9 10
11 12 13 14 15 16 17*
18 19 20 21 22 23 24
25 26 27 28 29 30 31

# Display any month
$ cal 03 2019
     03 - 2019     
Su Mo Tu We Th Fr Sa
                1  2
 3  4  5  6  7  8  9
10 11 12 13 14 15 16
17 18 19 20 21 22 23
24 25 26 27 28 29 30
31                  

# Display an entire year
$ cal 2000
                              Year 2000                           

     01 - 2000              02 - 2000              03 - 2000      
Su Mo Tu We Th Fr Sa   Su Mo Tu We Th Fr Sa   Su Mo Tu We Th Fr Sa
                   1          1  2  3  4  5             1  2  3  4
 2  3  4  5  6  7  8    6  7  8  9 10 11 12    5  6  7  8  9 10 11
 9 10 11 12 13 14 15   13 14 15 16 17 18 19   12 13 14 15 16 17 18
16 17 18 19 20 21 22   20 21 22 23 24 25 26   19 20 21 22 23 24 25
23 24 25 26 27 28 29   27 28 29               26 27 28 29 30 31   
30 31                                                             
     04 - 2000              05 - 2000              06 - 2000      
Su Mo Tu We Th Fr Sa   Su Mo Tu We Th Fr Sa   Su Mo Tu We Th Fr Sa
                   1       1  2  3  4  5  6                1  2  3
 2  3  4  5  6  7  8    7  8  9 10 11 12 13    4  5  6  7  8  9 10
 9 10 11 12 13 14 15   14 15 16 17 18 19 20   11 12 13 14 15 16 17
16 17 18 19 20 21 22   21 22 23 24 25 26 27   18 19 20 21 22 23 24
23 24 25 26 27 28 29   28 29 30 31            25 26 27 28 29 30   
30                                                                
     07 - 2000              08 - 2000              09 - 2000     
Su Mo Tu We Th Fr Sa   Su Mo Tu We Th Fr Sa   Su Mo Tu We Th Fr Sa
                   1          1  2  3  4  5                   1  2
 2  3  4  5  6  7  8    6  7  8  9 10 11 12    3  4  5  6  7  8  9
 9 10 11 12 13 14 15   13 14 15 16 17 18 19   10 11 12 13 14 15 16
16 17 18 19 20 21 22   20 21 22 23 24 25 26   17 18 19 20 21 22 23
23 24 25 26 27 28 29   27 28 29 30 31         24 25 26 27 28 29 30
30 31                                                             
     10 - 2000              11 - 2000              12 - 2000     
Su Mo Tu We Th Fr Sa   Su Mo Tu We Th Fr Sa   Su Mo Tu We Th Fr Sa
 1  2  3  4  5  6  7             1  2  3  4                   1  2
 8  9 10 11 12 13 14    5  6  7  8  9 10 11    3  4  5  6  7  8  9
15 16 17 18 19 20 21   12 13 14 15 16 17 18   10 11 12 13 14 15 16
22 23 24 25 26 27 28   19 20 21 22 23 24 25   17 18 19 20 21 22 23
29 30 31               26 27 28 29 30         24 25 26 27 28 29 30
                                              31                  
```
