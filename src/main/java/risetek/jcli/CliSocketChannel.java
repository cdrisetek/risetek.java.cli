package risetek.jcli;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class CliSocketChannel {

	public int port = 2223;

	// create server channel
	public void startServer() throws IOException {
		Selector sel = Selector.open();
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		InetSocketAddress isa = new InetSocketAddress("localhost", port);
		server.socket().bind(isa);
		System.out.println("Abt to block on select()");
		SelectionKey acceptKey = server.register(sel, SelectionKey.OP_ACCEPT);
		SocketChannel socket;
		while (acceptKey.selector().select() > 0) {

			Set<SelectionKey> readyKeys = sel.selectedKeys();
			Iterator<SelectionKey> it = readyKeys.iterator();

			while (it.hasNext()) {
				SelectionKey key = (SelectionKey) it.next();
				it.remove();

				if (key.isAcceptable()) {
					System.out.println("Key is Acceptable");
					ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
					socket = (SocketChannel) ssc.accept();
					socket.configureBlocking(false);
					new JCli(socket).start();
				}
			}
		}
	}
}
