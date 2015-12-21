#!/bin/sh

# NLM Archiving and Interchange 3.0
wget ftp://ftp.ncbi.nih.gov/pub/archive_dtd/archiving/archive-interchange-dtd-3.0.zip
unzip archive-interchange-dtd-3.0.zip
rm archive-interchange-dtd-3.0.zip
mv archiving nlm-archiving-dtd-3.0
cd nlm-archiving-dtd-3.0
sed 's/group xml:base.*/group /' < catalog-v3.xml > catalog.xml
cd ..

# JATS Archiving and Interchange 1.0
wget ftp://ftp.ncbi.nlm.nih.gov/pub/jats/archiving/1.0/jats-archiving-dtd-1.0.zip
unzip jats-archiving-dtd-1.0.zip -d jats-archiving-dtd-1.0
rm jats-archiving-dtd-1.0.zip

# JATS Archiving and Interchange 1.1d2
wget ftp://ftp.ncbi.nlm.nih.gov/pub/jats/archiving/1.1d2/JATS-Archiving-1.1d2-MathML2-DTD.zip
unzip JATS-Archiving-1.1d2-MathML2-DTD.zip
rm JATS-Archiving-1.1d2-MathML2-DTD.zip
rm Archiving-Readme.txt
mv JATS-Green-MathML2-1.1d2 jats-archiving-dtd-1.1d2

# JATS Archiving and Interchange 1.1d3
wget ftp://ftp.ncbi.nlm.nih.gov/pub/jats/archiving/1.1d3/JATS-Archiving-1-1d3-MathML2-DTD.zip
unzip JATS-Archiving-1-1d3-MathML2-DTD.zip
rm JATS-Archiving-1-1d3-MathML2-DTD.zip
rm Archiving-Readme.txt
mv JATS-Archiving-1-1d3-MathML2-DTD jats-archiving-dtd-1.1d3

