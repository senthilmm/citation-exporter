#!/opt/perl-5.16.2/bin/perl

# This perl script just hits the same service with the same URL, many times, and reports whenever there's
# an error.

use LWP::UserAgent;

my $req_url = 'http://xpmc11.be-md.ncbi.nlm.nih.gov:11999/?id=PMC3365160&idtype=pmcid&styles=modern-language-association';
my $ua = LWP::UserAgent->new;

for (my $i = 0; $i < 100; $i++) {
    my $req = HTTP::Request->new(GET => $req_url);
    my $resp = $ua->request($req);
    my $success = 1;
    my $error_msg;
    if (!$resp->is_success) {
        $success = 0;
        $error_msg = "Request '$req_url' failed: " . $resp->status_line;
        print "$error_msg\n";
    }
}


