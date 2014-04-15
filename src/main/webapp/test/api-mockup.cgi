#!/opt/perl-5.8.8/bin/perl -w
# Citation exporter mockup script.

use strict;
use warnings;

use FindBin;
use lib "$FindBin::Bin/lib";
use CGI;
use JSON;
use Data::Dumper;

# Create CGI object
my $q = CGI->new;


# Here is the list of query string parameters that are allowed
my @qs_param_names = qw( ids outputformat responseformat styles idtype report );
my %qs_param_names = map {$_ => 1} @qs_param_names;

# Read in the list of test cases from the test-cases.json file.
my $test_cases_json = do {
  local $/ = undef;
  open my $fh, "<", 'test-cases.json'
    or respond_error("Can't find test cases file!", 500);
  <$fh>;
};
my $test_cases;
eval {
  $test_cases = decode_json($test_cases_json)->{'test-cases'};
  1;
}
or do {
  respond_error("Unable to parse test cases JSON file", 500);
};

# If there are no params, return a page with the list of samples
my @pnames = $q->param;
if (!@pnames) {
  respond_index();
}

# Make sure all of the given query string params are allowed
my @given_qs_params = $q->param;
foreach my $gqp (@given_qs_params) {
  if (!$qs_param_names{$gqp}) {
    respond_error("Invalid query string parameter.");
  }
}

# Try to find a matching test case
my $match_case;
foreach my $tc (@$test_cases) {
  $match_case = $tc;  # Assume we have a match, until we find out otherwise

  # For each possible query string param, if it is defined in the test case, then
  # see if the request has a matching value
  foreach my $qs_param_name (@qs_param_names) {
    if (exists $tc->{$qs_param_name}) {
      my $qs_param = $q->param($qs_param_name);
      if (!defined $qs_param || $tc->{$qs_param_name} ne $qs_param) {
        $match_case = undef;
        last;
      }
    }
  }
  last if $match_case;
}

if ($match_case) {
  respond_file($match_case->{file});
}

# No matching case found
respond_error("No test case found");


# Respond with the index page
sub respond_index {
  print "Content-type: text/html\n\n";
  print <<END_HEAD;
<html>
  <head>
  </head>
  <body>
    <h1>Citation exporter samples</h1>
    <ul>
END_HEAD

#      <li>
#        <a href='?ids=PMC3362639&amp;report=full'>decription</a>
#      </li>
  foreach my $tc (@$test_cases) {
    my $desc = $tc->{description};
    my @params = map { exists $tc->{$_} ? "$_=" . $tc->{$_} : () } @qs_param_names;
    my $qs = join("&amp;", @params);
    print "<li><a href='?$qs'>$desc</a></li>\n";
  }
  print <<END_TAIL;
    </ul>
  </body>
</html>
END_TAIL
  exit 0;
}

# Respond with a file
sub respond_file {
  my $file = shift;
  (my $extension = $file) =~ s/.*\.//;
  my $media_type =
      $extension eq 'nxml' ? 'application/xml' :
      $extension eq 'pmfu' ? 'application/xml' :
      $extension eq 'json' ? 'application/json' :
      $extension eq 'html' ? 'text/html' :
      $extension eq 'rtf' ? 'text/rtf' :
      'text/plain';

  # Slurp in the file
  my $content = do {
    local $/ = undef;
    open my $fh, "<", $file
        or respond_error("Can't find file $file!", 500);
    <$fh>;
  };
  print $q->header($media_type);
  print $content;
  exit 0;
}

# To respond with an error.  Default status is 400, but if you include a second
# argument, you can override that (with, for example, 500).
sub respond_error {
  my $msg = shift;
  my $status = shift || 400;
  my $status_msg = $status == 400 ? "Bad request" : "Internal server error";

  print $q->header(-status => $status),
        $q->start_html($status_msg),
        $q->h2($status_msg),
        $q->p($msg),
        $q->end_html();
  exit 0;
}



