package risetek.jcli;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

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
		// RUN first onces

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				try {
					System.out.println("timer");
					loop(selectReason.TIMER);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 0, 1000);

		SelectionKey selectKey = _socket.register(sel, SelectionKey.OP_READ);
		boolean quit = false;
		while (!quit && selectKey.selector().select() > 0) {

			Set<SelectionKey> readyKeys = sel.selectedKeys();
			Iterator<SelectionKey> it = readyKeys.iterator();

			while (it.hasNext()) {
				SelectionKey key = (SelectionKey) it.next();
				it.remove();

				if (key.isReadable()) {
					if(cliState.CLI_QUIT == loop(selectReason.READ) ) {
						timer.cancel();
						key.cancel();
						sel.close();
						_socket.close();
						return false;
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

	interface confirmcallback {
		
	}
	interface confirmcontext {
		
	}
	
	enum selectReason {READ, TIMER}
	enum cliState {CLI_OK, CLI_QUIT, CLI_ERROR}
	enum State { STATE_LOGIN, STATE_PASSWORD, STATE_NORMAL, STATE_ENABLE_PASSWORD, STATE_ENABLE }
	enum cliMode {MODE_EXEC}
	private int flags = 0;
	private boolean showprompt = false;
	private byte oldcmd[] = null;
	private byte cmd[] = new byte[COMMAND_BUFFER_SIZE];
	private int oldl = 0;
	private int location = 0;
	private int cursor = 0;
	private confirmcallback cli_confirm_callback = null;
	private confirmcontext cli_confirm_context = null;
	private State state = State.STATE_NORMAL;
	private long idle_timeout = 0;
	private long last_action = 0;
	private int skip = 0;
	private byte is_telnet_option = 0;
	private byte esc;
	private byte lastchar;
	private int in_history;
	private static int MAX_HISTORY = 100;
	private static int COMMAND_BUFFER_SIZE = 1024;
	private int n;
	private cliMode mode;
	private boolean insertmode = false;
	private String username = null;
	private String password = null;
	private synchronized cliState loop(selectReason reason) throws IOException {
	    byte c = 0;
	    int onces = 0;

	    if(buflen == -1 && selectReason.READ == reason) {
			buf = ByteBuffer.allocate(1024);
			buflen = _socket.read(buf);
			if(buflen > 0)
				buf.flip();
			else {
				buflen = -1;
				return cliState.CLI_OK;
			}
		}
	    
	    
	    while(true) {

	    while(true) {

	    	if( onces++ > 0 ) {

	    		if( flags == 0 ) {
	    			flags = 1;
	    			showprompt = true;

	    			if (null != oldcmd)
	    			{
	    				location = cursor = oldl;
	    				oldcmd[location] = 0;
	    				showprompt = true;
	    				oldcmd = null;
	    				oldl = 0;
	    			}
	    			else
	    			{
	    				Arrays.fill(cmd, (byte)0);
	    				// memset(cli_state->cmd, 0, COMMAND_BUFFER_SIZE);
	    				location = 0;
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
	    	                write(cmd, 0, location);
	    	                if (cursor < location)
	    	                {
	    	                    int n = location - cursor;
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
//	    		return cliState.CLI_OK;
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
				// ��ʱ����£���ֹ����Ự
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
	    		return cliState.CLI_OK;
	    	}

	    	if(buflen == 0) {
	    		n = 0;
	    		buflen = -1;
	    		return cliState.CLI_OK;
	    	} else {
		    	n = 1;
		    	c = cli_read();
	    	}

			if (idle_timeout > 0)
				last_action = time();

			if (n == 0)
			{
				location = -1;
				break;
			}

			if (skip > 0)
			{
				skip--;
				continue;
			}

			// BY ycwang, ���cli_let��Ҫ���������ַ�������'Y/y', Ctrl-C�ȣ���ô�������䴦��
			// ���handler��Ϊ������ϣ���ô�����ͷ������������
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

			if (c == -1 && 0 == is_telnet_option)
			{
				is_telnet_option++;
				continue;
			}

			if (is_telnet_option != 0)
			{
				int cc = c + 256;
				if (cc >= 251 && cc <= 254)
				{
					is_telnet_option = c;
					continue;
				}

				if (c != -1)
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

					if (location == 0 || cursor == 0)
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
					if (location == 0 || cursor == 0)
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
						if (location == cursor)
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
								for (i = cursor; i <= location; i++) cmd[i] = cmd[i+1];
								write("\b");
								write(cmd[cursor]);
								write(" ");
								for (i = 0; i <= cmd.length - cursor; i++)
									write("\b");
							}
						}
						location--;
					}

					continue;
				}
			}

			/* redraw */
			if (c == CTRL('L'))
			{
				int i;
				int cursorback = location - cursor;

				if (state == State.STATE_PASSWORD || state == State.STATE_ENABLE_PASSWORD)
					continue;

				write("\r\n");
				show_prompt();
				write(cmd, 0, location);

				for (i = 0; i < cursorback; i++)
					write("\b");

				continue;
			}

			/* clear line */
			if (c == CTRL('U'))
			{
				if (state == State.STATE_PASSWORD || state == State.STATE_ENABLE_PASSWORD) {
					for(int index = 0; index < location; index++)
						cmd[index] = 0;
					// memset(cmd, 0, l);
				} else
					cli_clear_line(cmd, location, cursor);

				location = cursor = 0;
				continue;
			}

			/* kill to EOL */
			if (c == CTRL('K'))
			{
				if (cursor == location)
					continue;

				if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD)
				{
					int cc;
					for (cc = cursor; cc < location; cc++)
						write(" ");

					for (cc = cursor; cc < location; cc++)
						write("\b");
				}

				//memset(cmd + cursor, 0, l - cursor);
				for(int index = cursor; index < location; index++)
					cmd[index] = 0;
				location = cursor;
				continue;
			}

			/* EOT */
			if (c == CTRL('D'))
			{
				if (state == State.STATE_PASSWORD || state == State.STATE_ENABLE_PASSWORD)
					break;

				if (location > 0)
					continue;
				// TODO:
				// strcpy(cmd, "quit");
				Arrays.copyOf("quit".getBytes(), 4);
				location = cursor = "quit".length();
				write("quit\r\n");
				break;
			}

			/* disable */
			if (c == CTRL('Z'))
			{
				if (mode != cliMode.MODE_EXEC)
				{
					cli_clear_line(cmd, location, cursor);
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
					if (cursor < location)
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
				if (cursor < location)
				{
					if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD) {
						write(cmd, cursor, location - cursor);
					}
					cursor = location;
				}

				continue;
			}

			/* normal character typed */
			if (cursor == location)
			{
				 /* append to end of line */
				cmd[cursor] = (byte)c;
				if (location < (COMMAND_BUFFER_SIZE-1))
				{
					location++;
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
					if (location >= (COMMAND_BUFFER_SIZE-2)) location--;
					for (i = location; i >= cursor; i--)
						cmd[i + 1] = cmd[i];
					// Write what we've just added
					cmd[cursor] = (byte)c;

					write(cmd,cursor, location - cursor + 1);
					for (i = 0; i < (location - cursor + 1); i++)
						write("\b");
					location++;
				}
				else
				{
					cmd[cursor] = (byte)c;
				}
				cursor++;
			}

			if (state != State.STATE_PASSWORD && state != State.STATE_ENABLE_PASSWORD)
			{
				if (c == '?' && cursor == location)
				{
					write("\r\n");
					oldcmd = cmd;
					oldl = cursor = location - 1;
					break;
				}
				write(c);
			}

			oldcmd = null;
			oldl = 0;
			lastchar = c;
	    }

		flags = 0;

		if (location < 0) break;
	    //if (cmd.equals("quit")) break;
		String cmdString = new String(cmd, 0, location);
		if (cmdString.equals("quit")) break;

	    if (state == State.STATE_LOGIN)
	    {
	        if (location == 0) continue;

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
	        if (location == 0) continue;
	        if (cmd[location - 1] != '?' && cmd.equals("history"))
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
		// ����cli_let�ص�����, ��֪��cli�رգ�������������
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
	
	private byte CTRL(char c) {
		return (byte) ((byte)c-'@');
	}
	
	private class hide_command {
	}
	
	private boolean isspace(byte c) {
		return c == ' ';
	}

	String[] cli_parse_line(byte[] line)
	{
		List<String> words = new Vector<>();
		int index = 0;
		
//	    int nwords = 0;
//	    char *p = line;
	    int word_start = -1;
	    byte inquote = 0;

	    while (cmd[index] != 0)
	    {
	        if (!isspace(cmd[index]))
	        {
	            word_start = index;
	            break;
	        }
	        index++;
	    }

	    // while (nwords < max_words - 1)
	    while(true)
	    {
	        if ((cmd[index] == 0) || cmd[index] == inquote || (word_start!=-1 && (inquote==0) && (isspace(cmd[index]) || cmd[index] == '|')))
	        {
	            if (cmd[word_start] != 0)
	            {
	                // int len = index - word_start;
	                String word = new String(cmd, word_start, (index-word_start));
	                words.add(word);
	                //memcpy(words[nwords] = malloc(len + 1), word_start, len);
	                //words[nwords++][len] = 0;
	            }

	            if (cmd[index] == 0)
	                break;

	            if (inquote > 0)
	                index++; /* skip over trailing quote */

	            inquote = 0;
	            word_start = -1;
	        }
	        else if (cmd[index] == '"' || cmd[index] == '\'')
	        {
	            inquote = cmd[index]; index++; //*p++;
	            word_start = index;
	        }
	        else
	        {
	            if (word_start == -1)
	            {
	                if (cmd[index] == '|')
	                {
	                    //if (!(words[nwords++] = strdup("|")))   return 0;
	                	words.add("|");
	                }
	                else if (!isspace(cmd[index]))
	                    word_start = index;
	            }

	            index++;
	        }
	    }

	    //return nwords;
	    return words.toArray(new String[0]);
	}	
	private cliState cli_run_command(byte cmd[]) {
		System.out.println("Run cmd: " + new String(cmd));
		cliState r = cliState.CLI_OK;
		
		String[] words = cli_parse_line(cmd);
/*		
		    int num_words, i, f;
		    // 由于要push命令到这里面，所以我们要扩大这个空间。
		    char *words[128 * 2] = {0};
		    // int filters[128] = {0};
		    hide_command	auto_hide_commands = null;
		    int index = 0;
		    while(cmd[index] == ' ')
		    	index++;
		    
		    if (cmd[index] == 0) return cliState.CLI_OK;
		    // 最大只能占用一半
		    num_words = cli_parse_line(command, words, sizeof(words) / sizeof(words[0]) / 2);
		    
*/		    
		    
		    /*
		    for (i = f = 0; i < num_words && f < sizeof(filters) / sizeof(filters[0]) - 1; i++)
		    {
		        if (words[i][0] == '|')
		        filters[f++] = i;
		    }

		    filters[f] = 0;
*/
		
/*		
		    if (num_words)
				r = cli_find_command(cli, cli->mode, cli->common->commands, NULL, num_words, words, 0, filters, 0, &auto_hide_commands);
		    else
		        r = cliState.CLI_ERROR;

		    for (i = 0; i < num_words; i++)
		        free(words[i]);

		    // free_linked_hide_commands(&auto_hide_commands);
*/
		    return r;
	}
	
	private void cli_add_history(byte cmd[]) {
		
	}
	
	private long time() {
		return System.currentTimeMillis() / 100000;
	}
	
	private int cli_get_completions() {
		return 0;
	}
	
	ByteBuffer buf; // = ByteBuffer.allocate(1024);
	private int buflen = -1;
	private byte cli_read() throws IOException {
		byte b = buf.get();
		buflen--;
		return b;
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
