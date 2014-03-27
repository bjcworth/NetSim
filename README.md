NetSim
======

Go-Back-N Network Simulator (Java)

Compilation:
javac StudentNetworkSimulator.java

Write-up:
This code is designed to simulate the Go-Back-In protocol for servicing packets from a sender to a receiver. A sender class will attempt to transmit a number of packets to a receiver class with specified corruption/loss probability. It is up to the protocols I’ve implemented to ensure that such packets are delivered in order and without corruption. It accomplishes this by utilization of incremental sequencing and checksumming for every packet that is created and transmitted. The receiving entity then compares these sequences and checksums to those it expects to receive. If B receives something it doesn’t expect and it hasn’t ACK’d anything else yet, it will simply drop the packet. Otherwise, it will send a cumulative ACK for all packets it’s received thus far. If A receives a corrupt packet, it increments a corruption counter. Otherwise, it will move the sliding window over and stop the timer if this action made it so that the base = nextseqnum. If A receives nothing after a specified (static) interval input by the user, a timeout is triggered, during which A will retransmit all packets in the buffer that have been sent but not yet ACK’d. The timeout I used is one that was double the time of entered transmission time between layers. Once all packets sent have been accounted for, the simulation terminates, printing sent, retransmission, received, lost, and corruption counters.