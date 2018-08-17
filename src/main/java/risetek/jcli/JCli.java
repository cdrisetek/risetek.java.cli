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
	byte negotiate[] = {(byte)0xFF,(byte)0xFB,(byte)0x03,(byte)0xFF,(byte)0xFB,(byte)0x01,
			(byte)0xFF,(byte)0xFD,(byte)0x03,(byte)0xFF,(byte)0xFD,(byte)0x01};

	public JCli(SocketChannel socket) {
		_socket = socket;
	}

	private boolean schedule() throws IOException {
		write(negotiate);
		Selector sel = Selector.open();
/*
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				try {
					loop(selectReason.TIMER);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 0, 1000);
*/
		SelectionKey selectKey = _socket.register(sel, SelectionKey.OP_READ);
		// ByteBuffer buf = ByteBuffer.allocate(128);
		boolean quit = false;
		while (!quit && selectKey.selector().select() > 0) {

			Set<SelectionKey> readyKeys = sel.selectedKeys();
			Iterator<SelectionKey> it = readyKeys.iterator();

			while (it.hasNext()) {
				SelectionKey key = (SelectionKey) it.next();
				it.remove();

				if (key.isReadable()) {
					loop(selectReason.READ);
				}
			}
		}

//		timer.cancel();
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

	private int write(String string) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.put(string.getBytes());
		buf.flip();
		_socket.write(buf);
		return 0;
	}

	private int write(byte cmd[], int offset, int len) throws IOException {
		// TODO: trim string !!
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.put(cmd, offset, len);
		buf.flip();
		_socket.write(buf);
		return 0;
	}
	
	private int write(byte bytes[]) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.put(bytes);
		buf.flip();
		_socket.write(buf);
		return 0;
	}

	private void initcli() {
		// 状态初始化
		/*
		oldl = 0;
		cli_state->is_telnet_option = 0;
		cli_state->skip = 0;
		cli_state->esc = 0;
		cli_state->cursor = 0;
		cli_state->insertmode = 1;
		cli_state->oldcmd = NULL;
		cli_state->cmd = NULL;
		cli_state->in_history = 0;
		cli_state->lastchar = 0;
		cli_state->username = NULL;
		cli_state->password = NULL;
		cli_state->flags = 0;
		cli_state->quit_handler = quit_handler;
		cli_state->cli = cli;
		*/
	}
	
	interface confirmcallback {
		
	}
	interface confirmcontext {
		
	}
	
	enum selectReason {READ, TIMER}
	enum cliState {CLI_OK, CLI_QUIT, CLI_ERROR}
	enum State { STATE_LOGIN, STATE_PASSWORD, STATE_NORMAL, STATE_ENABLE_PASSWORD, STATE_ENABLE }
	enum cliMode {MODE_EXEC}
	private int flags;
	private boolean showprompt = false;
	private byte oldcmd[] = null;
	private byte cmd[] = new byte[COMMAND_BUFFER_SIZE];
	private int oldl = 0;
	private int l;
	private int cursor = 0;
	private confirmcallback cli_confirm_callback = null;
	private confirmcontext cli_confirm_context = null;
	private State state = State.STATE_NORMAL;
	private long idle_timeout = 0;
	private long last_action = 0;
	private int skip = 0;
	private char is_telnet_option = '\0';
	private char esc;
	private char lastchar;
	private int in_history;
	private static int MAX_HISTORY = 100;
	private static int COMMAND_BUFFER_SIZE = 1024;
	private int n;
	private cliMode mode;
	private boolean insertmode = false;
	private String username = null;
	private synchronized cliState loop(selectReason reason) throws IOException {

//		struct cli_state *cli_state = (struct cli_state *)argv;
//		struct cli_def *cli = cli_state->cli;
	    char c = 0;
	    int onces = 0;


	    while(true) {

	    while(true) {

	    	if( onces++ > 0 ) {

	    		if( flags == 0 ) {
	    			flags = 1;
	    			showprompt = true;

	    			if (null != oldcmd)
	    			{
	    				l = cursor = oldl;
	    				oldcmd[l] = 0;
	    				showprompt = true;
	    				oldcmd = null;
	    				oldl = 0;
	    			}
	    			else
	    			{
	    				// memset(cli_state->cmd, 0, COMMAND_BUFFER_SIZE);
	    				l = 0;
	    				cursor = 0;
	    			}
	    		}


	    		if ((showprompt) && (cli_confirm_callback == null)) {

	    	    if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD)
	    	    	write("\r\n");
	    	        switch (state)
	    	        {
	    	            case STATE_LOGIN:
	    	                write("Username: ");
	    	                break;

	    	            case STATE_PASSWORD:
	    	                write("Password: ");
	    	                break;

	    	            case STATE_NORMAL:
	    	            case STATE_ENABLE:
	    	                show_prompt();
	    	                write(cmd, 0, l);
	    	                if (cursor < l)
	    	                {
	    	                    int n = l - cursor;
	    	                    while (n-- > 0)
	    	                        write("\b");
	    	                }
	    	                break;

	    	            case STATE_ENABLE_PASSWORD:
	    	                write("Password: ");
	    	                break;

	    	        }

	    		showprompt = false;
			}
buflen = 0;
buf.flip();
	    		return cliState.CLI_OK;
	    	}

	    	if (reason == selectReason.TIMER )
	    	{
				/* timeout every second */
	    		/*
				if (cli->common->regular_callback && cli->common->regular_callback(cli) != CLI_OK)
				{
	    			strncpy(cmd, "quit", (COMMAND_BUFFER_SIZE-1));
					break;
				}
*/
				// 超时情况下，终止这个会话
	    		/*
				if(
				(0 != idle_timeout)
				&& ((((cli->common->users) || (cli->common->auth_callback)) && (state >= State.STATE_NORMAL))
				|| (((cli->common->enable_password) || (cli->common->enable_callback)) && (state >= State.STATE_ENABLE)))
				&& ((time() - last_action) >= idle_timeout) )
				{
					strcpy(cmd, "quit");
					l = cursor = cmd.length();
					break;
				}
				*/
				continue;
	    	}

	    	int rd = cli_read();
	    	if(rd == -1)
	    		n = rd;
	    	else if(rd == -2)
	    		n = 0;
	    	else {
	    		c = (char)rd;
	    		n = 1;
	    	}
	    	if(n < 0) {
	    		l = -1;
	    		break;
	    	}
	    		
/*	    	
			if ((n = read(cli->fd, &c, sizeof(c))) < 0)
			{
				if (errno == EINTR)
					continue;


				perror("read");
				l = -1;
				break;
			}
*/
			if (idle_timeout > 0)
				last_action = time();

			if (n == 0)
			{
				l = -1;
				break;
			}

			if (skip > 0)
			{
				skip--;
				continue;
			}

			// BY ycwang, 如果cli_let需要处理特殊字符，比如'Y/y', Ctrl-C等，那么我们让其处理，
			// 如果handler认为处理完毕，那么我们释放这个处理句柄。
			/*
			if( null != cli_confirm_callback ) {
				if(CLI_OK == (*cli->cli_confirm_callback)(cli, cli->cli_confirm_context, c, 0)) {
					cli_confirm_callback = null;
					cli_confirm_context = null;
					break;
				}
				continue;
			}
			*/

			if (c == (char)255 && 0 == is_telnet_option)
			{
				is_telnet_option++;
				continue;
			}

			if (is_telnet_option != 0)
			{
				if (c >= 251 && c <= 254)
				{
					is_telnet_option = c;
					continue;
				}

				if (c != 255)
				{
					is_telnet_option = 0;
					continue;
				}

				is_telnet_option = 0;
			}

			/* handle ANSI arrows */
			if (esc != 0)
			{
				if (esc == '[')
				{
					/* remap to readline control codes */
					switch (c)
					{
						case 'A': /* Up */
							c = CTRL('P');
							break;

						case 'B': /* Down */
							c = CTRL('N');
							break;

						case 'C': /* Right */
							c = CTRL('F');
							break;

						case 'D': /* Left */
							c = CTRL('B');
							break;

						default:
							c = 0;
					}

					esc = 0;
				}
				else
				{
					esc = (c == '[') ? c : 0;
					continue;
				}
			}

			if (c == 0) continue;
			if (c == '\n') continue;

			if (c == '\r')
			{
				if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD)
					write("\r\n");
				break;
			}

			if (c == 27)
			{
				esc = 1;
				continue;
			}

			if (c == CTRL('C'))
			{
				write("\0xa");
				continue;
			}

			/* back word, backspace/delete */
			if (c == CTRL('W') || c == CTRL('H') || c == 0x7f)
			{
				int back = 0;

				if (c == CTRL('W')) /* word */
				{
					int nc = cursor;

					if (l == 0 || cursor == 0)
						continue;

					while (nc > 0 && cmd[nc - 1] == ' ')
					{
						nc--;
						back++;
					}

					while (nc > 0 && cmd[nc - 1] != ' ')
					{
						nc--;
						back++;
					}
				}
				else /* char */
				{
					if (l == 0 || cursor == 0)
					{
						write("\0xa");
						continue;
					}

					back = 1;
				}

				if (back != 0)
				{
					while (back-- > 0)
					{
						if (l == cursor)
						{
							cmd[--cursor] = 0;
							if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD)
								write("\b \b");
						}
						else
						{
							int i;
							cursor--;
							if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD)
							{
								for (i = cursor; i <= l; i++) cmd[i] = cmd[i+1];
								write("\b");
								write(cmd[cursor]);
								write(" ");
								for (i = 0; i <= cmd.length - cursor; i++)
									write("\b");
							}
						}
						l--;
					}

					continue;
				}
			}

			/* redraw */
			if (c == CTRL('L'))
			{
				int i;
				int cursorback = l - cursor;

				if (state == State.STATE_PASSWORD || state == State.STATE_ENABLE_PASSWORD)
					continue;

				write("\r\n");
				show_prompt();
				write(cmd, 0, l);

				for (i = 0; i < cursorback; i++)
					write("\b");

				continue;
			}

			/* clear line */
			if (c == CTRL('U'))
			{
				if (state == State.STATE_PASSWORD || state == State.STATE_ENABLE_PASSWORD) {
					// memset(cmd, 0, l);
				} else
					cli_clear_line(cmd, l, cursor);

				l = cursor = 0;
				continue;
			}

			/* kill to EOL */
			if (c == CTRL('K'))
			{
				if (cursor == l)
					continue;

				if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD)
				{
					int cc;
					for (cc = cursor; cc < l; cc++)
						write(" ");

					for (cc = cursor; cc < l; cc++)
						write("\b");
				}

				//memset(cmd + cursor, 0, l - cursor);
				l = cursor;
				continue;
			}

			/* EOT */
			if (c == CTRL('D'))
			{
				if (state == State.STATE_PASSWORD || state == State.STATE_ENABLE_PASSWORD)
					break;

				if (l > 0)
					continue;
				// TODO:
				// strcpy(cmd, "quit");
				l = cursor = cmd.length;
				write("quit\r\n");
				break;
			}

			/* disable */
			if (c == CTRL('Z'))
			{
				if (mode != cliMode.MODE_EXEC)
				{
					cli_clear_line(cmd, l, cursor);
					cli_set_configmode(cliMode.MODE_EXEC, null, null);
					showprompt = true;
				}

				continue;
			}

			/* TAB completion */
			/*
			if (c == CTRL('I'))
			{
				char completions = new char[128];
				int num_completions = 0;

				if (state == State.STATE_LOGIN || state == State.STATE_PASSWORD || state == State.STATE_ENABLE_PASSWORD)
					continue;

				if (cursor != l) continue;

				num_completions = cli_get_completions(cli, cmd, completions, 128);
				if (num_completions == 0)
				{
					write("\0xa");
				}
				else if (num_completions == 1)
				{
					// Single completion
					for (; l > 0; l--, cursor--)
					{
						if (cmd[l-1] == ' ' || cmd[l-1] == '|')
							break;
						write("\b");
					}
					strcpy((cmd + l), completions[0]);
					l += strlen(completions[0]);
					cmd[l++] = ' ';
					cursor = l;
					write(completions[0]);
					write(" ");
				}
				else if (lastchar == CTRL('I'))
				{
					// double tab
					int dd = getsamecharlen(completions,num_completions);
					int j;

					for (j = 0;j < dd&&j < l; j ++)
					{
						if (cmd[l-1-j] == ' ' || cmd[l-1-j] == '|')
							break;
					}

					if(dd > j)
					{
							for (; l > 0; l--, cursor--)
							{
								if (cmd[l-1] == ' ' || cmd[l-1] == '|')
									break;
								write(cli->fd, "\b", 1);
							}
							strncpy((cmd + l), completions[0],dd);
							l += dd;
							cursor = l;
							write(cli->fd, completions[0], dd);
					}
					else
					{
							int i;
							write(cli->fd, "\r\n", 2);
							for (i = 0; i < num_completions; i++)
							{
								write(cli->fd, completions[i], strlen(completions[i]));
							if (i % 4 == 3)
									write("\r\n");
								else
									write("     ");
							}
							if (i % 4 != 3) write("\r\n");
								showprompt = 1;
					}
					}
					else
					{
					// More than one completion
					lastchar = c;
					write("\0xa");
					}
				continue;
			}
*/
			/* history */
			/*
			if (c == CTRL('P') || c == CTRL('N'))
			{
				int history_found = 0;

				if (state == State.STATE_LOGIN || state == State.STATE_PASSWORD || state == State.STATE_ENABLE_PASSWORD)
					continue;

				if (c == CTRL('P')) // Up
				{
					in_history--;
					if (in_history < 0)
					{
						for (in_history = MAX_HISTORY-1; in_history >= 0; in_history--)
						{
							if (cli->history[in_history])
							{
								history_found = 1;
								break;
							}
						}
					}
					else
					{
						if (cli->history[in_history]) history_found = 1;
					}
				}
				else // Down
				{
					in_history++;
					if (in_history >= MAX_HISTORY || !cli->history[in_history])
					{
						int i = 0;
						for (i = 0; i < MAX_HISTORY; i++)
						{
							if (cli->history[i])
							{
								in_history = i;
								history_found = 1;
								break;
							}
						}
					}
					else
					{
						if (cli->history[in_history]) history_found = 1;
					}
				}
				if (history_found && cli->history[in_history])
				{
					// Show history item
					cli_clear_line(cli->fd, cmd, l, cursor);
					memset(cmd, 0, COMMAND_BUFFER_SIZE);
					strncpy(cmd, cli->history[in_history], (COMMAND_BUFFER_SIZE-1));
					l = cursor = strlen(cmd);
					write(cli->fd, cmd, l);
				}

				continue;
			}
*/
			/* left/right cursor motion */
			if (c == CTRL('B') || c == CTRL('F'))
			{
				if (c == CTRL('B')) /* Left */
				{
					if (cursor > 0)
					{
						if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD)
							write("\b");

						cursor--;
					}
				}
				else /* Right */
				{
					if (cursor < l)
					{
						if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD) {
							char cmdc = (char) cmd[cursor];
							write(cmdc);
						}
						cursor++;
					}
				}

				continue;
			}

			/* start of line */
			if (c == CTRL('A'))
			{
				if (cursor > 0)
				{
					if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD)
					{
						write("\r");
						show_prompt();
					}

					cursor = 0;
				}

				continue;
			}

			/* end of line */
			if (c == CTRL('E'))
			{
				if (cursor < l)
				{
					if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD) {
						write(cmd, cursor, l - cursor);
					}
					cursor = l;
				}

				continue;
			}

			/* normal character typed */
			if (cursor == l)
			{
				 /* append to end of line */
				cmd[cursor] = (byte)c;
				if (l < (COMMAND_BUFFER_SIZE-1))
				{
					l++;
					cursor++;
				}
				else
				{
					write("\0xa");
					continue;
				}
			}
			else
			{
				// Middle of text
				if (insertmode)
				{
					int i;
					// Move everything one character to the right
					if (l >= (COMMAND_BUFFER_SIZE-2)) l--;
					for (i = l; i >= cursor; i--)
						cmd[i + 1] = cmd[i];
					// Write what we've just added
					cmd[cursor] = (byte)c;

					write(cmd,cursor, l - cursor + 1);
					for (i = 0; i < (l - cursor + 1); i++)
						write("\b");
					l++;
				}
				else
				{
					cmd[cursor] = (byte)c;
				}
				cursor++;
			}

			if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD)
			{
				if (c == '?' && cursor == l)
				{
					write("\r\n");
					oldcmd = cmd;
					oldl = cursor = l - 1;
					break;
				}
				write(c);
			}

			oldcmd = null;
			oldl = 0;
			lastchar = c;
	    }

		flags = 0;

		if (l < 0) break;
	    if (cmd.equals("quit")) break;

	    if (state == State.STATE_LOGIN)
	    {
	        if (l == 0) continue;

	        /* require login */
	        /*
	        free_z(cli_state->username);
	        if (!(cli_state->username = strdup(cmd)))
	            return cliState.CLI_ERROR;
	            */
	        username = new String(cmd);
	        state = State.STATE_PASSWORD;
	        showprompt = true;
	    }
	    else if (state == State.STATE_PASSWORD)
	    {
	        /* require password */
	        int allowed = 0;
/*
	        free_z(cli_state->password);
	        if (!(cli_state->password = strdup(cmd)))
	            return cliState.CLI_ERROR;
		    if (cli->common->auth_callback)
	        {
				if (cli->common->auth_callback(cli_state->username, cli_state->password) == CLI_OK)
	                allowed++;
	        }

	        if (!allowed)
	        {
	            struct unp *u;
				for (u = cli->common->users; u; u = u->next)
	            {
	                if (!strcmp(u->username, cli_state->username) && pass_matches(u->password, cli_state->password, cli_state->username))
	                {
	                    allowed++;
	                    break;
	                }
	            }
	        }

	        if (allowed)
	        {
	            cli_error(" ");
	            state = State.STATE_NORMAL;
	        }
	        else
	        {
	            cli_error("\n\nAccess denied");
	            free_z(cli_state->username);
	            free_z(cli_state->password);
	            state = State.STATE_LOGIN;
	        }

	        showprompt = 1;
	        */
	    }
	    else if (state == State.STATE_ENABLE_PASSWORD)
	    {
	    	/*
	        int allowed = 0;
			if (cli->common->enable_password)
	        {
	            // check stored static enable password
				if (pass_matches(cli->common->enable_password, cmd, ENABLE_KEY))
	                allowed++;
	        }

			if (!allowed && cli->common->enable_callback)
	        {
	            // check callback
				if (cli->common->enable_callback(cmd))
	                allowed++;
	        }

	        if (allowed)
	        {
	            state = State.STATE_ENABLE;
	            cli_set_privilege(cli, PRIVILEGE_PRIVILEGED);
	        }
	        else
	        {
	            cli_error("\n\nAccess denied");
	            state = State.STATE_NORMAL;
	        }
	        */
	    }
	    else
	    {
	        if (l == 0) continue;
	        if (cmd[l - 1] != '?' && cmd.equals("history"))
	            cli_add_history(cmd);

	        if (cli_run_command(cmd) == cliState.CLI_QUIT) {
	        	break;
	        }
	    }

		// TODO: FIXME history
		if(!showprompt) {
		    in_history = 0;
		    lastchar = 0;
		}

	    // Update the last_action time now as the last command run could take a
	    // long time to return
	    if (idle_timeout > 0)
	        last_action = time();
	    }

	    /*
		cli_quit_handler_t *quit_handler = cli_state->quit_handler;
		pevent_unregister(&cli->read);
		pevent_unregister(&cli->checker);
	     */
		// 处理cli_let回调函数, 告知其cli关闭，让其清理环境。
		/*
		if( cli->cli_confirm_callback )
			(*cli->cli_confirm_callback)(cli, cli->cli_confirm_context, 0, 1);
*/
		close_monitor();
/*
		fflush(cli->client);
		fclose(cli->client);
		cli->client = 0;

		cli_free_history(cli);
		free_z(cli_state->username);
		free_z(cli_state->password);
		free_z(cmd);
		free_z(cli_state);
		// FIXME!!
		free_z(cli->promptchar);
		free_z(cli->modestring);
		free_z(cli->domain);

		// close file handler and clean up cli.
		(*quit_handler)(cli);
*/
		return cliState.CLI_QUIT;
	}
	
	private void show_prompt() throws IOException {
		write("prompt:");
	}
	
	private void cli_error(String message) {
		System.out.println("TODO: cli_error");
	}
	
	private void close_monitor() {
		System.out.println("TODO: close_monitor");
	}
	
	private char CTRL(char c) {
		return (char) (c-'@');
	}
	
	private cliState cli_run_command(byte cmd[]) {
		return cliState.CLI_OK;
	}
	
	private void cli_add_history(byte cmd[]) {
		
	}
	
	private long time() {
		return System.currentTimeMillis() / 100000;
	}
	
	private int cli_get_completions() {
		return 0;
	}
	
	ByteBuffer buf = ByteBuffer.allocate(1024);
	private int buflen = 0;
	private int cli_read() throws IOException {
		if(buflen == 0) {
		buflen = _socket.read(buf);
		if(buflen > 0)
			buf.flip();
		}
		if(buflen == 0)
			return -2;
		
		int c = 0;
		byte b = buf.get();
		buflen--;
		c = b >= 0 ? b : (0xff+b+1);
		return c;
	}
	
	private void cli_clear_line(byte cmd[], int start, int end) {
		
	}
	
	private void cli_set_configmode(cliMode mode, String a, String b) {
		
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
