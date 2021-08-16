#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# -gcutil 0 250 5
#
#  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT   
#  0.00  64.15   0.00   0.00  95.71  84.70      5     0.258     0     0.000     0     0.000     0.258
#  0.00  64.15   0.00   0.00  95.71  84.70      5     0.258     0     0.000     0     0.000     0.258
#  0.00  64.15   0.00   0.00  95.71  84.70      5     0.258     0     0.000     0     0.000     0.258
#  0.00  64.15   0.00   0.00  95.71  84.70      5     0.258     0     0.000     0     0.000     0.258
#  0.00  64.15   0.00   0.00  95.71  84.70      5     0.258     0     0.000     0     0.000     0.258
#
# -J-XX:+UseParallelGC -gcutil 0 250 5
#
#  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT   
#  0.00 100.00  46.54  14.99  94.73  89.25      5     0.197     0     0.000     -         -     0.197
#  0.00 100.00  46.54  14.99  94.73  89.25      5     0.197     0     0.000     -         -     0.197
#  0.00 100.00  48.50  14.99  94.73  89.25      5     0.197     0     0.000     -         -     0.197
#  0.00 100.00  48.50  14.99  94.73  89.25      5     0.197     0     0.000     -         -     0.197
#  0.00 100.00  50.46  14.99  94.73  89.25      5     0.197     0     0.000     -         -     0.197

BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	}

/^  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT   $/	{
	    headerlines++;
	}

/^[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*([0-9]+|-)[ ]*([0-9]+\.[0-9]+|-)[ ]*[0-9]+\.[0-9]+$/	{
	    datalines++;
	}

	{ totallines++; print $0 }

END	{
	    if ((headerlines == 1) && (datalines == 5)) {
	        exit 0
            }
            else {
	        exit 1
            }
	}
