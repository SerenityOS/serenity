#
#
#

locale ja JP JP
new instance jcal
new gregorian gcal

# Use GMT+09:00 for max day of year test which depends on time zone
# offsets.

timezone GMT+09:00
new instance tokyocal

test Make sure that the maximum year value doesn't depent on era
    use jcal
	# Note: the max year value is as of Reiwa
	assign 292276976 $max
	clear all
	set date Reiwa 1 May 1
	get millis
	check max year $max
	assign $max $maxyear

	clear all
	set date Heisei 20 May 5
	get millis
	check max year $maxyear

	clear all
	set date Showa 35 May 5
	get millis
	check max year $maxyear

	clear all
	set date BeforeMeiji 1 Jun 1
	get millis
	check max year $max

test Max of ERA
    use jcal
	# Assumption: Reiwa is the current era
	check maximum era Reiwa
	check leastmax era Reiwa

test Actual max MONTH
    use jcal
	clear all
	set date BeforeMeiji 1867 Jan 31
	check actualmax month Dec
	# Make sure that the same value is returned after
	# normalization.
	get millis
	check actualmax month Dec

	clear all
	set date Meiji 45 Mar 31
	check actualmax month Jul
	get millis
	check actualmax month Jul

	clear all
	set date Taisho 15 June 1
	check actualmax month Dec
	get millis
	check actualmax month Dec

	clear all
	set date Showa 64 Jan 4
	check actualmax month Jan
	get millis
	check actualmax month Jan

	clear all
	set date Heisei 31 Jan 4
	check actualmax month Apr
	get millis
	check actualmax month Apr

	clear all
	set date Reiwa 2 Jan 1
	set year $maxyear
	check actualmax month Aug
	get millis
	check actualmax month Aug

	clear all
	set date 17 Mar 1
	check actualmax month Dec
	get millis
	check actualmax month Dec

test Actual max DAY_OF_YEAR
    use jcal
	clear all
	set date Meiji 1 Dec 31
	# Meiji Gan-nen is a leap year.
	check actualmax day_of_year 366
	check day_of_year 366

	clear all
	set date Meiji 45 Jan 1
	# Meiji 45 or Taishi Gan-nen is also a leap year.
	check actualmax day_of_year 211 # 31+29+31+30+31+30+29
	set date Meiji 45 Jul 29
	check day_of_year 211
	set date Taisho 1 Jul 31
	get millis
	check actualmax day_of_year 155 # 366 - 211
	set date Taisho 1 Dec 31
	check day_of_year 155

	clear all
	set date Taisho 15 Sep 23
	check actualmax day_of_year 358 # 365 - 7
	set date Taisho 15 Dec 24
	check day_of_year 358
	set date Showa 1 Dec 25
	check actualmax day_of_year 7
	set date Showa 1 Dec 31
	check day_of_year 7

	clear all
	set date Showa 64 Jan 3
	check actualmax day_of_year 7
	set date Showa 64 Jan 7
	check day_of_year 7
	set date Heisei 1 Aug 9
	check actualmax day_of_year 358 # 365 - 7
	set date Heisei 1 Dec 31
	check day_of_year 358

   # time zone dependent
   use tokyocal
	clear all
	set date Reiwa $maxyear Jan 1
	# the last date of Reiwa is R292276976.08.17T16:12:55.807+0900
	check actualmax day_of_year 229 # 31+28+31+30+31+30+31+17

test Actual max WEEK_OF_YEAR
    use jcal
	clear all
	set date Meiji 1 Jan 1
	# Meiji gan-nen is a leap year.
	check actualmax week_of_year 52

	clear all
	set date Meiji 45 Jan 1
	check actualmax week_of_year 30
	set date Taisho 1 July 31
	check actualmax week_of_year 22

	clear all
	set date Taisho 15 Sep 23
	check actualmax week_of_year 51
	set date Showa 1 Dec 25
	check actualmax week_of_year 1

	clear all
	set date Showa 64 Jan 3
	check actualmax week_of_year 1
	set date Heisei 1 Aug 9
	check actualmax week_of_year 51

	clear all
	set date Heisei 31 Apr 28
	check actualmax week_of_year 17
	set date Reiwa 1 Aug 9
	check actualmax week_of_year 35

    use tokyocal
	set date Reiwa $maxyear Jan 1
	# the last date of Reiwa is R292276976.08.17T16:12:55.807+0900 (Sunday)	
	# The year is equivalent to 2003 (Gregorian).
	check actualmax week_of_year 34

test Actual max WEEK_OF_MONTH
    use jcal
	clear all
	set date Meiji 45 Jul 1
	check actualmax week_of_month 5
	set date Taisho 1 Jul 31
	check actualmax week_of_month 5

	clear all
	set date Taisho 15 Dec 1
	check actualmax week_of_month 5
	set date Showa 1 Dec 25
	check actualmax week_of_month 5

	clear all
	set date Showa 64 Jan 1
	check actualmax week_of_month 5
	set date Heisei 1 Jan 8
	check actualmax week_of_month 5

	clear all
	set date Heisei 31 Apr 30
	check actualmax week_of_month 5
	set date Reiwa 1 May 1
	check actualmax week_of_month 5

    use tokyocal
	set date Reiwa $maxyear Jan 1
	# the last date of Reiwa is R292276976.08.17T16:12:55.807+0900 (Sunday)	
	# The year is equivalent to 2003 (Gregorian).
	check actualmax week_of_month 4
	
test Actual max DAY_OF_WEEK_IN_MONTH
    use jcal
	clear all
	set date Meiji 45 Jul 1
	check actualmax week_of_month 5
	set date Taisho 1 Jul 31
	check actualmax week_of_month 5

	clear all
	set date Taisho 15 Dec 1
	check actualmax week_of_month 5
	set date Showa 1 Dec 25
	check actualmax week_of_month 5

	clear all
	set date Showa 64 Jan 1
	check actualmax week_of_month 5
	set date Heisei 1 Jan 8
	check actualmax week_of_month 5

	clear all
	set date Heisei 31 Apr 30
	check actualmax week_of_month 5
	set date Reiwa 1 May 1
	check actualmax week_of_month 5

    use tokyocal
	clear all
	set date Reiwa $maxyear Jan 1
	# the last date of Reiwa is R292276976.08.17T16:12:55.807+0900 (Sunday)	
	# The year is equivalent to 2003 (Gregorian).
	check actualmax week_of_month 4

test Actual max YEAR
    use jcal
	clear all
	set date BeforeMeiji 1 Jan 1
	check actualmax year 1867

	set date Meiji 1 Jan 1
	check actualmax year 45

	set date Meiji 1 Jul 30
	check actualmax year 44

	set date Taisho 1 Jul 30
	check actualmax year 15

	set date Taisho 1 Dec 25
	check actualmax year 14

	set date Showa 2 Jan 1
	check actualmax year 64

	set date Showa 1 Dec 25
	check actualmax year 63

	set date Heisei 1 Jan 7
	check actualmax year 64 

	set date Heisei 1 Aug 18
	check actualmax year 30

	set date Reiwa 1 Apr 30
	check actualmax year 31

	# Date/time beyond the last date in the max year.
	set date Reiwa 1 Aug 18
	check actualmax year 292276975
	
test Least max YEAR
	set date Heisei 17 Mar 1
	# Taisho is the shortest era, 14 years.
	# (See above actual max YEAR case.)
	check leastmax year 14

test Acutual min YEAR
	# Get minimum values for comparison
	clear all
	set era BeforeMeiji
	get min year
	assign $result $minyear
	set date $minyear Dec 31
	eval $minyear + 1
	assign $result $minyear_plus_one

	# BeforeMeiji 1 Dec 31 should exist in the minimum year which
	# should be the same value as the getMinimum() value.
	set date BeforeMeiji 1 Dec 31
	check actualmin year $minyear

	# Jan 1 shouldn't exist in the same year. So the actual minimum is
	# $minyear + 1.
	set date 1 Jan 1
	check actualmin year $minyear_plus_one

	# 1 should be returned if it's on a date of the last
	# year which also exists in the first year of each era.
	clear all
	set date Meiji 45 Jan 1
	check actualmin year 1

	clear all
	set date Taisho 14 Jul 30
	check actualmin year 1

	clear all
	set date Showa 60 Dec 25
	check actualmin year 1

	clear all
	set date Heisei 17 Jan 8
	check actualmin year 1

	# 2 should be returned if it's on a date of the last year which
	# doesn't exist in the first year of each era. (Meiji is an
	# exception.)
	clear all
	set date Taisho 14 Jul 29
	check actualmin year 2

	clear all
	set date Showa 60 Dec 23
	check actualmin year 2

	clear all
	set date Heisei 17 Jan 7
	check actualmin year 2
