#!/usr/bin/perl

use strict;
use LWP::UserAgent;

my @sample_ids;
open (my $SAMPLES, "random-pmcids.txt") or die "Can't find sample ids file";
while (my $line = <$SAMPLES>) {
    chomp $line;
    push @sample_ids, $line;
}
close $SAMPLES;
my $num_samples = scalar @sample_ids;

print "Num samples = $num_samples\n";
print "sample_ids[0] = $sample_ids[0]\n";

my $ua = LWP::UserAgent->new;

my $base_url = 'http://www.ncbi.nlm.nih.gov/pmc/utils/ctxp/?style=apa&ids=';
for (my $i = 0; $i < $num_samples; $i++) {
    my $req_url = $base_url . $sample_ids[$i];


    print time . ": Requesting '$req_url'\n";

    my $req = HTTP::Request->new(GET => $req_url);
    my $resp = $ua->request($req);
    my $success = 1;
    my $error_msg;
    if (!$resp->is_success) {
        $success = 0;
        $error_msg = "Request '$req_url' failed: " . $resp->status_line;
        print "  $error_msg\n";
    }
    else {
        print "  Success: $req_url\n";
    }

    sleep 1;
}
