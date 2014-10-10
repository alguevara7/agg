# counters-clj

A Clojure library designed to ... well, that part is up to you.

## Usage

TBD

## TODO

- (X) integrated with graphite !

- use normal distribution for simulation

- integrate with kafka !
  - use one node for now

- use two kafka nodes (will need to figure out leader and stuff like that)

- (X) exit when put is closed

- need to write unit tests

- retrieve events in batches

- figure out creation of channels ... agg should handle it ...

- stop when outout channel is closed

- handle exceptions

- TODO flush changed entries only ! need a set to store keys, flush when count >= n.

- graphite!

- write changed counters only

- need to publish aggregation batches of a certain size  (the batch size is how many keys there are in state) that controls how much data will be written to the db in one transaction

- allow events to be fetched in batches

- only write counters that changed

- do-agg ---> flush on exit

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
