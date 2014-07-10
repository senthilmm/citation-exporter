#!/opt/perl-5.16.2/bin/perl

use strict;
use warnings;
$|++;

use Getopt::Long;
use Time::HiRes qw/time/;
use LWP::UserAgent;
use Data::Dumper;

#-----------------------------------------------------------------
# Parse command line options, output usage help, etc.

my $usage = q(
Usage: performance-test.pl [options]

Options (any of these can be abbreviated):
--help|-? - print this usage message and exit
--verbose
--num - number of requests for each test. This can be any number up to the the number of
  ids stored in random-aiids.txt, currently 100,000. Default is 1000.
--test=<test> - select which test to run.  Default is to run all. Tests are:
    - pub-one
    - citeproc
    - 1-style
    - 3-styles
--ignore-errors - continue when error (such as HTTP 404) is encountered.  Default is to abort.

Specify the address of the service under test:
--port - IP port number of the service.  Default is 11999.
--base-url - base URL of the service.  No default.

How IDs are selected for use:
--idtype <idtype> - either aiid (default) or pmcid.  Default is aiid.
--random - select random sample of ids each time (default is pseudo-random, which will be the same
  set every time you run the test). Exclusive with --aiid.
--id <id> - use the given ID for every request.  Exclusive with -r.
);


my $help = 0;
my $verbose = 0;
my $num_reqs = 1000;
my $num_forks = 10;
my $selected_test = 'all';
my $ignore_errors = 0;
my $port = 11999;
my $base_url = '';
my $idtype = '';
my $random = 0;
my $req_id = '';

GetOptions(
  "help|?"        => \$help,
  "verbose"       => \$verbose,
  "num=i"         => \$num_reqs,
  "forks=i"       => \$num_forks,
  "test=s"        => \$selected_test,
  "ignore-errors" => \$ignore_errors,
  "port=i"        => \$port,
  "base-url=s"    => \$base_url,
  "idtype=s"      => \$idtype,
  "random"        => \$random,
  "id=s"          => \$req_id,
);

if ($help) {
    print $usage;
    exit 0;
}

# Set default base_url
if ($base_url eq '') {
    print "Missing required parameter base-url.\n";
    exit 1;
}

# Fix up id and idtype
# If `id` was given but `idtype` was not, then compute idtype
if ($req_id ne '' && $idtype eq '') {
    if ($req_id =~ /^\d+$/) {
        $idtype = 'aiid';
    }
    elsif ($req_id =~ /^PMC[0-9.]+/) {
        $idtype = 'pmcid';
    }
    else {
        command_line_error("Can't decipher the type of ID '$req_id'");
    }
}
# Implement default value for $idtype
$idtype = 'aiid' if $idtype eq '';
# Validate $idtype
if ($idtype ne 'aiid' && $idtype ne 'pmcid') {
    command_line_error("Invalid idtype: '$idtype'");
}


# Prepare the web stuff
my $ua = LWP::UserAgent->new;

# Read in master list of sample ids.  Each element will be an array of [aiid, pmcid]
my $sample_ids = [];
open (my $SAMPLES, "random-aiids.txt") or die "Can't find sample ids file";
my $line = <$SAMPLES>;   # discard header
while (my $line = <$SAMPLES>) {
    next if $line !~ /^(\d+)\s+(\w+)/;
    push @$sample_ids, [$1, $2];
}
close $SAMPLES;
my $num_samples = scalar @$sample_ids;

my @tests = ( 'echotest', 'pub-one', 'citeproc', '1-style', '3-styles', );
my %tests = (
    'echotest' => $base_url . 'echotest',
    'pub-one'  => $base_url . '?id={id}&idtype={idtype}&outputformat=pub-one',
    'citeproc' => $base_url . '?id={id}&idtype={idtype}&outputformat=citeproc',
    '1-style'  => $base_url . '?id={id}&idtype={idtype}',
    '3-styles' => $base_url .
        '?id={id}&idtype={idtype}&styles=modern-language-association,apa,chicago-author-date',
);

print "          |          |  Time   |    Throughput     |         Latency                   |\n" .
      "  Test    | Requests | Elapsed | sec/req | req/sec |  Min   |  Ave   |  Max   | StdDev | Num errs\n" .
      "----------|----------|---------|---------|---------|--------|--------|--------|--------|----------\n";


for my $test (@tests) {
    next if ($selected_test ne 'all' && $test ne $selected_test);

    # Note that they are already in pseudo-random order.  But, if the `-r` switch was given, shuffle
    shuffle($sample_ids) if $random;


    my $r = run_test($test);
    my $n = $r->{num_reqs} - $r->{num_errs};
    my $l_sum = $r->{l_sum};
    my $l_ave = $r->{l_sum} / $n;
    my $l_stddev_str = ' undef';
    if ($n > 1) {
        my $l_variance = ($r->{l_sum_sq} - $l_sum * $l_sum / $n) / ($n - 1);
        $l_stddev_str = sprintf("%6.3f", sqrt($l_variance));
    }
    printf("%8s  | %7d  | %7.1f | %7.3f | %7.3f | %6.3f | %6.3f | %6.3f | %s | %4d\n",
           $r->{test}, $r->{num_reqs}, $r->{delta}, $r->{time_req}, $r->{req_sec},
           $r->{l_min}, $l_ave, $r->{l_max}, $l_stddev_str,
           $r->{num_errs});
}


#########################################################################################
sub command_line_error {
    my $msg = shift;
    print "$msg\n" . $usage;
    exit 1;
}


#########################################################################################
# This returns a hash with the test results:
#   General info
#     test - the name of the test
#   Overall statistics
#     num_reqs - total number of requests
#     num_errs - number of requests that resulted in an error
#     delta - total elapsed time, in seconds
#     time_req - total (throughput) time / request
#     req_sec - throughput: average number of requests / second
#   Statistics on requests
#     l_min - minimum latency
#     l_max (req_time_max) - maximum latency
#     l_sum - used to compute average latency
#     l_sum_sq - used to compute standard deviation

sub run_test {
    my $test = shift;
    my $test_results = {
        test           => $test,
        num_reqs       => $num_reqs,
        num_errs       => 0,
        l_max    => 0,
        l_min    => 99999,
        l_sum    => 0,
        l_sum_sq => 0,
    };

    my $start_time = time;
    my $req_url_t = $tests{$test};  # request url template

    # This stores data about the child processes.  The keys are the pids.  The values are
    # hashes that store the req_num and the reader glob.
    my $child_process_data = {};

    # In an earlier version, we kept track of requests that failed, and didn't count those against
    # the total; so that $num_reqs was always the total number of _successful_ requests sent.  When
    # implementing multiple processes sending simultaneous requests, this gets more complicated,
    # because at the time we send a request, we don't know the total number of current requests
    # that would succeed or fail.  So, that that feature was removed.

    # If $num_forks == 1, then we don't do any forking.  Everything runs as one process.
    if ($num_forks == 1) {
        for (my $req_num = 0; $req_num < $num_reqs; ++$req_num) {
            my $req_url = req_url($req_url_t, $req_id, $idtype, $sample_ids, $req_num);
            my $req_result = do_request($req_url);
            record_req_result($test_results, $req_result);
        }
    }
    else {
        for (my $req_num = 0; $req_num < $num_reqs; ++$req_num) {

            # If necessary, wait for one of the child processes to finish
            while (scalar keys %$child_process_data >= $num_forks) {
                print "Waiting for a child process to finish\n" if $verbose;
                my $done_pid = wait();
                harvest($child_process_data, $done_pid, $test_results);
            }

            # Fork a new process, connecting a child's writer to the parent's reader
            pipe(my $reader, my $writer);
            my $child_pid = fork;
            if ($child_pid) {
                print "Parent, forked $child_pid\n" if $verbose;
                close $writer;
                $child_process_data->{$child_pid} = {
                    req_num => $req_num,
                    reader => $reader,
                };
            }
            elsif ($child_pid == 0) {
                print "Child:$req_num started\n" if $verbose;
                close $reader;

                # Compute the URL to use
                my $req_url = req_url($req_url_t, $req_id, $idtype, $sample_ids, $req_num);
                my $result = do_request($req_url);
                my $result_str = $req_num . " " . $result->{success} . " " . $result->{'time'};
                if ($result->{error_msg}) {
                    $result_str .= ' ' . $result->{error_msg};
                }
                print $writer $result_str;
                print "Child:$req_num returning '$result_str'\n" if $verbose;
                exit 0;
            }
            else {
                die "Couldn't fork: $!";
            }
        }

        # Wait for all the straggler child processes to finish
        while (scalar keys %$child_process_data > 0) {
            my $done_pid = wait();
            harvest($child_process_data, $done_pid, $test_results);
        }
    }

    my $delta = time - $start_time;
    my $req_sec = $delta == 0 ? 99999 : $num_reqs / $delta;

    $test_results->{delta} = $delta;
    $test_results->{time_req} = $delta / $num_reqs;
    $test_results->{req_sec} = $req_sec;

    return $test_results;
}

#########################################################################################
# Compute the URL to use for an individual request.  This uses a URL template, $req_url_t,
# and finds values to substitute in for {id} and {idtype}.

sub req_url {
    my ($req_url_t, $req_id, $idtype, $sample_ids, $req_num) = @_;

    # If the id was given as a command line argument, use that.  Otherwise, get one from the samples
    my $id = $req_id ne '' ? $req_id : $sample_ids->[$req_num][$idtype eq 'aiid' ? 0 : 1];
    my $req_url = $req_url_t;
    $req_url =~ s/\{id\}/$id/;
    $req_url =~ s/\{idtype\}/$idtype/;
    return $req_url;
}

#########################################################################################
# Record the results of one request into the $test_results object.  The $req_result is
# the hash returned from do_request.

sub record_req_result {
    my ($test_results, $req_result) = @_;
    my $success = $req_result->{success};
    my $req_time = $req_result->{'time'};
    my $error_msg = $req_result->{error_msg};
    if (!$success && !$ignore_errors) {
        #print Dumper($req_result);
        die "Child process reports error from HTTP request" . ($error_msg ? ": '$error_msg'" : "");
    }
    if ($success) {
        $test_results->{l_min} = $req_time if $req_time < $test_results->{l_min};
        $test_results->{l_max} = $req_time if $req_time > $test_results->{l_max};
        $test_results->{l_sum} += $req_time;
        $test_results->{l_sum_sq} += $req_time * $req_time;
    }
    else {
        $test_results->{num_errs}++ if !$success;
    }
}

#########################################################################################
sub harvest {
    my ($child_process_data, $done_pid, $test_results) = @_;
    my $process_data = $child_process_data->{$done_pid};  # data for this child
    my $child_req_num = $process_data->{$done_pid};
    my $reader = $process_data->{reader};
    my $child_result_str = <$reader>;
    print "Parent: child $done_pid results: '$child_result_str'\n" if $verbose;
    close $reader;
    delete $child_process_data->{$done_pid};

    # Parse out the results from the string returned by the child process
    $child_result_str =~ /^(\S+) (\S+) (\S+)( (.*))?$/;
    record_req_result($test_results, {
        success => $2,
        'time' => $3,
        error_msg => $5,
    });
}

#########################################################################################
# This sends a single request to the server, and times it.  It returns a hash that includes
# whether or not the request was successful, and the time that it took, as a floating point
# in units of seconds.

sub do_request {
    my $req_url = shift;

    print "Requesting '$req_url'\n" if $verbose;

    my $req_start = time;
    my $req = HTTP::Request->new(GET => $req_url);
    my $resp = $ua->request($req);
    my $success = 1;
    my $error_msg;
    if (!$resp->is_success) {
        $success = 0;
        $error_msg = "Request '$req_url' failed: " . $resp->status_line;
        if ($ignore_errors && $verbose) {
            print "$error_msg\n";
        }
    }
    return {
        success   => $success,
        'time'    => time - $req_start,
        error_msg => $error_msg,
    };
}



#########################################################################################
sub shuffle {
    my $deck = shift;  # $deck is a reference to an array
    return unless @$deck; # must not be empty!

    my $i = @$deck;
    while (--$i) {
        my $j = int rand ($i+1);
        @$deck[$i,$j] = @$deck[$j,$i];
    }
}
