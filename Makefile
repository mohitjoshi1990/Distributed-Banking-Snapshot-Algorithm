all: clean
	mkdir bin
	javac -classpath /home1/vchaska1/protobuf/protobuf-java-3.5.1.jar -d bin/ src/BranchController.java src/Branch.java src/Bank.java src/BranchSender.java src/BranchReceiver.java

clean:
	rm -rf bin/