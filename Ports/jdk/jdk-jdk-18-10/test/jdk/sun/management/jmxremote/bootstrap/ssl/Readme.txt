The trustore and keystore are to be generated as follows:

1. keytool -genkey -alias duke -keyalg RSA -keysize 2048 -validity 36500 -keystore keystore -storepass password
- use password 'password' for the keystore and key passwords
- leave all values at default
- the certificate validity will be 100 years (should be enough for now)
2. keytool -export -keystore keystore -alias duke -file duke.crt
3. keytool -import -alias duke -file duke.crt -keystore truststore -storepass trustword
- use password 'trustword' for the keystore and key passwords
- leave all values at default
