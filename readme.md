## Raft

> 分布式一致性算法



### 节点类型

+ leader：和 client 交互接受日志条目并管理
+ pre candidate：
+ candidate：leader不存在时，进行选举成为候选人
+ follower：不主动发送请求



### RPC

#### AppendEntries RPC

> leader → follower，作为发送心跳和同步日志使用（区别：心跳不携带日志数据 com.siesta.raft.proto.RaftProto.LogEntry#getData）

**流程**

+ leader收到来自client的消息，整理出log日志，并发送给follower（并行发送、失败重试、但不保证一定收到正确的返回）
+ client判断前面log的一致性和term，成功将log复制，并返回响应
+ leader发现超过一半的follower已经成功复制，将日志committed（apply到statement machine）
+ 后续的appendEntries RPC中，follower发现请求的commitIndex比自己的commitIndex大，会将自己这部分日志进行committed

leader 发送消息携带 term、target server 前一个已经复制的log index、target server 前一个已经复制的log term、leader 已经 commit 的 log index

注：commitIndex、matchIndex 和 nextIndex 的区别：commitIndex 是已经 commit 的日志下标；matchIndex 是 leader 确认已经 replicate 的日志下标（response后）；nextIndex 是下一个将要复制的日志下标

什么是 replicate？简单说就是集群之间的日志拷贝，Log数据可以存在内存、磁盘

什么是 commit？将日志提交给了状态机，执行相关的操作，在java中可以理解为提交给JVM处理

作为分布式一致性算法，appendEntries需要判断两边数据的一致性。以下情况需要处理：

+ follower的term大于leader传过来的term？

  leader的term更新为follower的term，同时leader变为follower

+ follower保存的leaderId与leader传递过来的leaderId不一致？

  

+ follower保存的log日志和leader的认为的日志不一致？

  + Log同步数量不一致：`prevLogIndex != follower.getLastIndex`
  + Log数据不一致：`prevLogIndex == follower.getLastIndex && prevLogIndex != follower.getLastTerm`

  leader降低nextIndex，并重新发送

+ 



#### VoteRequest RPC

pre 和 vote 操作类似，合并在一起

> 选举成为leader的RPC

pre参考问题二

选举是否超过半数的server选票给该节点，不仅仅是两个节点具有连通性，同时也要判断term和log：

+ candidate必须包含全部已经committed日志，其他节点收到candidate包含日志信息的请求，发现日志没有自己的新，会拒绝请求
+ candidate的term必须大于等于其他节点的term





### Raft实现中的问题

#### 一、如何成为一个leader？

集群中没有leader，server在 election timeout 的时间内没有收到心跳，会变为candidate，并发送 requestVote RPC，超过半数的 server 投票给他后，成为leader

#### 二、为什么需要在 candidate 前添加 pre Candidate？

在 raft 是这样描述的，当 leader 不存在时，follower 会变为 candidate 并选举成为 leader

但是 follower 没法通过心跳判断是 leader 挂了，还是其他原因

举个例子：follower 可以成功发送请求，却无法收到来自其他节点的请求，在 election timeout 后，成为 candidate，会将原本正常的 leader 给顶掉（因为变为candidate后 term++，当 leader 接收到比自己大的 term，会变为 follower），而这个节点本身无法收到其他节点的请求，这个candidate也无法成为leader，会导致集群很长时间不可用。

#### 三、如何实现 election timeout没有收到心跳发起 preVote？

一开始我想到的是使用定时器，但是定时器没法做到收到心跳后，直接结束本次任务。

参考 raft-java，采用延时的方式是一个很好的方法，创建一个延时任务（preVote）如果收到了心跳，就删除这个延时任务，重新创建一个新的延时任务，如果 election timeout了，就会执行延时任务。

对应：`com.siesta.raft.RaftServer#startElectionTimer`

#### 四、如何避免多个Server成为candidate？

先说明为什么要避免多个Server，多个Server竞选有可能导致选票被均分（一般自己会先投自己一票），谁都无法成为leader，导致集群长时间不可用

election timeout 每次采用随机值的方式，这样各个Server成为候选人的时间各不相同，能够尽量避免选票被瓜分。

对应：`com.siesta.raft.RaftServer#getElectionTimeout`



### 未完成部分

+ 重定向：client将消息发送到follower，需要将消息重定向发送到leader
+ term自动随时间增加？
+ 





### 参考文档

[Raft论文 (qq.com)](https://docs.qq.com/doc/DY0VxSkVGWHFYSlZJ)

[wenweihu86/raft-java: Raft Java implementation which is simple and easy to understand. (github.com)](https://github.com/wenweihu86/raft-java)

