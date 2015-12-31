#!/bin/sh
# This script runs from Maven's generate-resources phase.

# NLM Archiving and Interchange 3.0
if [ ! -d "nlm-archiving-dtd-3.0" ]; then
    wget ftp://ftp.ncbi.nih.gov/pub/archive_dtd/archiving/archive-interchange-dtd-3.0.zip
    unzip archive-interchange-dtd-3.0.zip
    rm archive-interchange-dtd-3.0.zip
    mv archiving nlm-archiving-dtd-3.0
    cd nlm-archiving-dtd-3.0
    sed 's/group xml:base.*/group /' < catalog-v3.xml > catalog.xml
    cd ..
fi

# JATS Archiving and Interchange 1.0
if [ ! -d "jats-archiving-dtd-1.0" ]; then
    wget ftp://ftp.ncbi.nlm.nih.gov/pub/jats/archiving/1.0/jats-archiving-dtd-1.0.zip
    unzip jats-archiving-dtd-1.0.zip -d jats-archiving-dtd-1.0
    rm jats-archiving-dtd-1.0.zip
fi

# JATS Archiving and Interchange 1.1d1
if [ ! -d "jats-archiving-dtd-1.1d1" ]; then
    wget ftp://ftp.ncbi.nlm.nih.gov/pub/jats/archiving/1.1d1/JATS-Archiving-1.1d1-MathML2-DTD.zip
    unzip JATS-Archiving-1.1d1-MathML2-DTD.zip
    rm JATS-Archiving-1.1d1-MathML2-DTD.zip
    rm Archiving-Readme.txt
    mv JATS-Archiving-1.1d1-MathML2-DTD jats-archiving-dtd-1.1d1
fi


# JATS Archiving and Interchange 1.1d2
if [ ! -d "jats-archiving-dtd-1.1d2" ]; then
    wget ftp://ftp.ncbi.nlm.nih.gov/pub/jats/archiving/1.1d2/JATS-Archiving-1.1d2-MathML2-DTD.zip
    unzip JATS-Archiving-1.1d2-MathML2-DTD.zip
    rm JATS-Archiving-1.1d2-MathML2-DTD.zip
    rm Archiving-Readme.txt
    mv JATS-Green-MathML2-1.1d2 jats-archiving-dtd-1.1d2
fi

# JATS Archiving and Interchange 1.1d3
if [ ! -d "jats-archiving-dtd-1.1d3" ]; then
    wget ftp://ftp.ncbi.nlm.nih.gov/pub/jats/archiving/1.1d3/JATS-Archiving-1-1d3-MathML2-DTD.zip
    unzip JATS-Archiving-1-1d3-MathML2-DTD.zip
    rm JATS-Archiving-1-1d3-MathML2-DTD.zip
    rm Archiving-Readme.txt
    mv JATS-Archiving-1-1d3-MathML2-DTD jats-archiving-dtd-1.1d3
fi

