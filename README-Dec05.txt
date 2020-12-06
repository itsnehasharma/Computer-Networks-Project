to run this program, 

first run 

java Peer <server port> <0> <peer num> <isOwner=1>
example:
java Peer 15123 0 1001 1 //1 means owner, run this person first 

then as many as you want: 
java Peer <server port> <client port> <peer num> <isOwner=0>

make sure that whatever next peers you run, a server is already open for that peer.
for example, 

java Peer 15124 15123 1002 0 //is valid because a server is running on 15123
java Peer 15126 15124 1002 0 //is NOT valid because no server is running on 15124

FOR NOW, the owner sends the entire file to the other peers that join. this will be fixed later. 



