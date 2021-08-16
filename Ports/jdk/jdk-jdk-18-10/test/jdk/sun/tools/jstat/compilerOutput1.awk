#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# Compiled Failed Invalid   Time   FailedType FailedMethod
#       38      0       0     0.41          0             



BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	}

/^Compiled Failed Invalid   Time   FailedType FailedMethod$/	{
	    headerlines++;
	}

# note - the FailedMethod column is not matched very thoroughly by the
# following pattern. We just check for zero or more spaces after the
# FailedType column and the for any sequence of characters for the
# FailedMethod column. Better checking would verify an optional string of
# characters that follows class/method name patterns. However, it's very
# difficult to generate any data in this column under normal circumstances.
#
/^[ ]*[0-9]+[ ]*[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*.*$/	{
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
