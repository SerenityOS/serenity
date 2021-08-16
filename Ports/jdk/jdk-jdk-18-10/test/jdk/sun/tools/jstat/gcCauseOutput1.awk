#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# -gccause 0
#
#  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT    LGCC                 GCC                 
#  0.00  54.68   0.00   0.00  94.02  84.11      4     0.269     0     0.000     0     0.000     0.269 G1 Evacuation Pause  No GC               
#
# -J-XX:+UseSerialGC -gccause 0
#
#  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT    LGCC                 GCC                 
# 78.14   0.00  68.23  15.57  94.69  88.17      4     0.204     0     0.000     -         -     0.204 Allocation Failure   No GC               

BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	}

/^  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT    LGCC                 GCC                 $/	{
	    headerlines++;
	}

# The following pattern does not verify the validity of the gc cause
# string as the values can vary depending on conditions out of our
# control. To accomodate this variability, the pattern matcher simply
# detects that there are two strings that match a specific pattern
# where the first character is a letter followed by a sequence of zero
# or more letters and spaces. It also provides for the ".", "(", and ")"
# characters to allow for the string "System.gc()".
#
/^[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*([0-9]+\.[0-9]+)|-[ ]*([0-9]+\.[0-9]+)|-[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*([0-9]+)|-[ ]*([0-9]+\.[0-9]+)|-[ ]*[0-9]+\.[0-9]+[ ]*[a-zA-Z]+[a-zA-Z \.\(\)]*[ ]*[a-zA-Z]+[a-zA-Z \.\(\)]*$/	{
	    datalines++;
	}

	{ totallines++; print $0 }

END	{
	    if ((headerlines == 1) && (datalines == 1)) {
	        exit 0
	    }
	    else {
	        exit 1
	    }
	}
