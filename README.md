# PDF processor

Java desktop application to extract a sting matching a pattern from a 
PDF file.
Every PDF input files is transliterated to text (Apache PDFBox) and 
then, using pattern matching, you are able to search anything you want.
The output is a CSV file (Apache Commons CSV) with patterns in columns 
and data of the file in rows.

## Usage

1. Setup the required library
1. Prepare the .property file
1. Launch the application

## Description of property file fields 

* debug=**false** - [true/false] enable/disable debug print messages
* rotation_degree=**0** - [0-360] rotate the PDF input file of the specified degrees before transliterate it
* TXT_enabled=**false** - [true/false] enable/disable TXT transliterated text file creation (same filename of source)
* TXT_encoding=**UTF-8** - encoding of TXT file
* TXT_append=**false** - [true/false] overwrite (default) or append
* ETL_from=**\\r\\n|\\r|\\n** - [regex] transform the selected text with pattern
* ETL_to=**\ ** - [text] transform the selected text to text
* filename_entry=**filename** - CSV column with filename
* CSV_filename=**output.csv** - output filename
* patterns_prefix=**pattern.** - prefix of the following patterns
* pattern.1=**[text\[A-z]\*, text2\[A-z]\*]** - list of regex to match in order for column "1"
* copyPDF=**true** - [true/false] enable/disable copyPDF feature
* PDFformat=**[1, 2]** - [list] copyPDF: filename format (use field 1 and 2)
* copyPDFsep=**\ ** - [text] copyPDF: filename fiels separator (default space)
* copyPDFETL_from=**/** - [regex] copyPDF: replace source regex (default dash not allowed in filename)
* copyPDFETL_to=**.** - [text] copyPDF: replace destination string (default dot)

## Dependency

Main library components:

* [Java SE version 1.7](http://www.oracle.com/technetwork/java/javase/)
* [Apache PDFBox](https://pdfbox.apache.org/) 2.0.6+
* [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/) 
1.4+

Complete list of "**lib/*.jar**":

    libs/commons-csv-1.4.jar
    libs/commons-io-2.5.jar
    libs/commons-logging-1.2.jar
    libs/fontbox-2.0.6.jar
    libs/pdfbox-2.0.6.jar
    libs/pdfbox-tools-2.0.6.jar

