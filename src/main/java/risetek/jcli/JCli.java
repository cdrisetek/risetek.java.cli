package risetek.jcli;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class JCli implements Runnable {
	SocketChannel _socket;

	public JCli(SocketChannel socket) {
		_socket = socket;
	}

	private boolean schedule() throws IOException {
		Selector sel = Selector.open();

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				try {
					process(0, selectReason.TIMER);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 0, 1000);

		SelectionKey selectKey = _socket.register(sel, SelectionKey.OP_READ);
		ByteBuffer buf = ByteBuffer.allocate(128);
		boolean quit = false;
		while (!quit && selectKey.selector().select() > 0) {

			Set<SelectionKey> readyKeys = sel.selectedKeys();
			Iterator<SelectionKey> it = readyKeys.iterator();

			while (it.hasNext()) {
				SelectionKey key = (SelectionKey) it.next();
				it.remove();

				if (key.isReadable()) {
					SocketChannel socket = (SocketChannel) key.channel();
					try {
						int len = socket.read(buf);
						if(len < 0) {
							key.cancel();
							quit = true;
							break;
						} else {
							buf.flip();
							for(int index=0;index<len;index++) {
								int c = (int)buf.get();
								process(c, selectReason.READ);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		timer.cancel();
		sel.close();
		return false;
	}

	private int write(int c) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(10);
		buf.put((byte)c);
		buf.flip();
		_socket.write(buf);
		return 0;
	}

	enum selectReason {READ, TIMER}
	private synchronized void process(int c, selectReason reason) throws IOException {
		if(selectReason.READ == reason) {
			write(c);
		} else if(selectReason.TIMER == reason) {
			
		}
	}
	
	@Override
	public void run() {
		try {
			while(schedule());
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("end of cli");
	}
}
