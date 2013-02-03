#java -javaagent:deuceAgent-1.2.0.jar -cp . Main --key-range=1000000 --read-pct=0
#java -javaagent:deuceAgent-1.2.0.jar -cp . Verify --key-range=1000000 --read-pct=0
java -javaagent:deuceAgent-1.2.0.jar -cp . Verify

#java -javaagent:deuceAgent-1.2.0.jar -cp . Main
