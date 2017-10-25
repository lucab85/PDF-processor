#!/bin/bash

if [ "$#" -ne 1 ]; then
	echo "Usage: $0 <ErrorProne path>"
	echo "see http://errorprone.info/docs/installation"
	exit 1
fi

ERRORPRONEPATH=$1

java -Xbootclasspath/p:$1 -classpath "libs/*" com.google.errorprone.ErrorProneCompiler src/pdfp/FileProcessor.java
