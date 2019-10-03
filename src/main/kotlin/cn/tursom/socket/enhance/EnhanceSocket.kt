package cn.tursom.socket.enhance

interface EnhanceSocket<Read, Write> : SocketReader<Read>, SocketWriter<Write>