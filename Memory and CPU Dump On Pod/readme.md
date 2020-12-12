## memory and cpu dump on pod

**TCP Dump**
```ruby
nsenter -t 5436 -n -- tcpdump -i any --nn -w /host/tmp/(pod_name).pcap
```
**Memory Dump**
```ruby
oc exec mypodname -- jcmd 1 GC.heap_dump /tmp/heap.hprof
oc rsync mypodname:/tmp/heap.hprof /tmp/.
oc exec mypodname  -- rm /tmp/heap.hprof 
```
**Thread Dump**
```ruby
oc exec mypodname -- jcmd 1 Thread.print > /tmp/Threadprint.txt
oc rsync mypodname:/tmp/heap.hprof /tmp/.
oc exec mypodname -- rm /tmp/heap.hprof 
```
**Garbage Collections**
```ruby
oc exec mypodname -- jcmd 1 GC.class_histogram > /tmp/GC_class_histogram.txt
```
**Top Result**
```ruby
oc exec mypodname -- top -b -n 1 -H -p 1 > /tmp/top.out
```
**Jstat Result**
```ruby
oc exec mypodname -- jstat -gcutil 1 10000 5 >  /tmp/jstat.out
```
