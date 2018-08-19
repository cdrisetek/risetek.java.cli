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

	private static int PRIVILEGE_UNPRIVILEGED = 0; 
	
	CliCallback cli_int_history = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.print("do command:"+command);
			return cliState.CLI_OK;
		}
		
	};
	
	CliCallback cli_int_help = new CliCallback() {

		@Override
		public cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException {
			cli.print("do command:"+command + " start with:" + start + " argc:" + argc);
			return cliState.CLI_OK;
		}
		
	};
	
	public JCli(SocketChannel socket) {
		_socket = socket;
		
		
		cli_register_command(null, "history", cli_int_history, PRIVILEGE_UNPRIVILEGED, MODE_EXEC, "Display the session command history");		
		cli_register_command(null, "help", cli_int_help, PRIVILEGE_UNPRIVILEGED, MODE_EXEC, "Display the help of commands");		
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
					// System.out.println("timer");
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
	enum cliState {CLI_OK, CLI_QUIT, CLI_ERROR, CLI_ERROR_ARG}
	enum State { STATE_LOGIN, STATE_PASSWORD, STATE_NORMAL, STATE_ENABLE_PASSWORD, STATE_ENABLE }
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
				if (mode != MODE_EXEC)
				{
					cli_clear_line(cmd, location, cursor);
					cli_set_configmode(MODE_EXEC, null, null);
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
	
	private void cli_error(String format, Object...args ) throws IOException{
		if(_helper_print != null)
			_helper_print.print(format, args);
		// System.out.println(String.format(fmt+"\r\n", objects));
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

	List<String> cli_parse_line(byte[] line)
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
	    return words;
	}	
	private cliState cli_run_command(byte cmd[]) throws IOException {
		System.out.println("Run cmd: " + new String(cmd));
		cliState r = cliState.CLI_OK;
	    hide_command	auto_hide_commands = null;
	    int index = 0;
	    while(cmd[index] == ' ')
	    	index++;
	    
	    if (cmd[index] == 0) return cliState.CLI_OK;
		
		List<String> words = cli_parse_line(cmd);
		if(words.size() == 0)
			return cliState.CLI_ERROR;
		r = cli_find_command(mode, common.commands, null, words, 0, null, 0, auto_hide_commands);
/*		
		    int num_words, i, f;
		    // 由于要push命令到这里面，所以我们要扩大这个空间。
		    char *words[128 * 2] = {0};
		    // int filters[128] = {0};
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
				r = cli_find_command(mode, cli->common->commands, NULL, num_words, words, 0, filters, 0, &auto_hide_commands);
		    else
		        r = cliState.CLI_ERROR;

		    for (i = 0; i < num_words; i++)
		        free(words[i]);

		    // free_linked_hide_commands(&auto_hide_commands);
*/
		    return r;
	}
	
	cliMode MODE_ANY = new cliMode();
	cliMode MODE_EXEC = new cliMode();
	cliMode MODE_CONFIG = new cliMode();

	private cliMode mode = MODE_EXEC;
	
	private class cliMode {
		cliMode parent;
	}
	
	interface Wilds_callback {
		int call(cli_command command, String word);
	}
	interface CliCallback {
		cliState call(JCli cli, String command, List<String> words, int start, int argc) throws IOException;
	}
	interface IHelper_print {
		void print(String format, Object ...args) throws IOException;
	}
	
	// enum Privilege {}
	private class cli_command {
		cli_command children;
		cli_command next;
		int push_command;

		String command;
		Wilds_callback wilds_callback;
		CliCallback callback;
		int privilege;
		private cliMode mode;
		String help;
	}
	private class Common {
		cli_command commands;
		cli_filter	filters;
	}
	
	private class cli_filter {
		
	}
	private Common common = new Common();
	private int privilege;
	private cliState cli_find_command(cliMode mode, cli_command commands, CliCallback havecallback,
			List<String> words, int start_word, List<String> filters,int wilds, hide_command auto_hide_commands)
					throws IOException {
		/*
	    struct cli_command *c, *again = NULL;
	    int c_words = num_words;

	    if (filters[0])
	        c_words = filters[0];
*/
		int c_words = words.size();
		cli_command c, again=null;
		if(words.size() < start_word)
			return cliState.CLI_ERROR;
	    //if (!words[start_word])	        return CLI_ERROR;

	    // Deal with ? for help
	    //if (words[start_word][strlen(words[start_word]) - 1] == '?')
		if(words.get(start_word).endsWith("?"))
	    {
	        //int l = strlen(words[start_word])-1;
	        int l = words.get(start_word).length() -1;
	        
			//words[start_word][strlen(words[start_word]) - 1] = '\0';

	        for (c = commands; c != null; c = c.next)
	        {
			    if ( ((c.wilds_callback != null && (c.wilds_callback.call(c, words.get(start_word)) != 0))
			    || !strncasecmp(c.command, words.get(start_word), l))
	                && (c.callback!=null || c.children!=null)
	                && privilege >= c.privilege
				&& (c.mode == mode || c.mode == MODE_ANY)
				&& !link_hide_command(auto_hide_commands, c) )
			    {
						cli_print_callback(helper_print);
	                    cli_error("  %-20s %s", c.command, c.help==null ? null : c.help);
						cli_print_callback(null);
			    }
	        }

			if( commands == null ){
				if( l == 0 )
					cli_error("  %-20s", "<cr>");
				else
					cli_error("  %-20s", "Unrecognized command");
			}
			else
			{
				if (havecallback != null && ( l == 0 ))
					cli_error("  %-20s", "<cr>");
			}

	        return cliState.CLI_OK;
	    }
//	    else if(words[start_word][strlen(words[start_word]) - 1] == '!')
	    else if(words.get(start_word).endsWith("!"))
	    {
			//fix the '!' conf bug 20130220
			if(mode == MODE_CONFIG)
				return cliState.CLI_OK;
			// TODO:!!!
			//cli_set_configmode(mode->parent, null, null);
			return cliState.CLI_OK;
	    }

	    for (c = commands; c!=null; c = c.next)
	    {

	        if (privilege < c.privilege)
	            continue;

			if( c.wilds_callback != null )
			{
				if(c.wilds_callback.call(c, words.get(start_word))!=0)
					continue;
			}
			else
			{
				if (strncasecmp(c.command, words.get(start_word), get_unique_len(commands, c)))
		            continue;

		        if (strncasecmp(c.command, words.get(start_word), words.get(start_word).length()))
		            continue;
			}

			if(link_hide_command(auto_hide_commands, c) )  continue;

			// 通配符匹配的命令压入到命令表中。
			if( c.wilds_callback != null)
			{
				// words[num_words+wilds] = words[start_word];
				wilds++;
			}
			// 特别的命令需要提供给执行函数的压入到命令表中。
			if( c.push_command != 0)
			{
				// words[num_words+wilds] = c->command;
				wilds++;
			}

			if (c.mode == mode || c.mode == MODE_ANY )
			{
				cliState rc = cliState.CLI_OK;
				int f;
				cli_filter filt = common.filters;
/*
				// Found a word!
				if (start_word == c_words - 1)
				{
					// TODO:!!!
					if (c.callback != null)						goto CORRECT_CHECKS;

					cli_error("Incomplete command");
					return cliState.CLI_ERROR;
				}
				rc = cli_find_command(mode, c.children, c.callback, words, start_word + 1, filters,wilds, auto_hide_commands);
				return rc;
*/
				// Found a word!
				if (start_word != c_words - 1) {
					rc = cli_find_command(mode, c.children, c.callback, words, start_word + 1, filters,wilds, auto_hide_commands);
					return rc;
				}

				if (c.callback == null) {
					cli_error("Incomplete command");
					return cliState.CLI_ERROR;
				}
/*
				for (f = 0; rc == cliState.CLI_OK && filters[f]; f++)
				{
					int n = num_words;
					char **argv;
					int argc;
					int len;

					if (filters[f+1])
					n = filters[f+1];

					if (filters[f] == n - 1)
					{
						cli_error(cli, "Missing filter");
						return CLI_ERROR;
					}

					argv = words + filters[f] + 1;
					argc = n - (filters[f] + 1);
					len = strlen(argv[0]);
					if (argv[argc - 1][strlen(argv[argc - 1]) - 1] == '?')
					{
						if (argc == 1)
						{
							int i;

							for(i = 0; filter_cmds[i].cmd; i++)
							{
								cli_error(cli, "  %-20s %s", filter_cmds[i].cmd, filter_cmds[i].help );
							}
						}
						else
						{
							if (argv[0][0] != 'c') // count
								cli_error(cli, "  WORD");

							if (argc > 2 || argv[0][0] == 'c') // count
								cli_error(cli, "  <cr>");
						}

						return CLI_OK;
					}

					if (argv[0][0] == 'b' && len < 3) // [beg]in, [bet]ween
					{
						cli_error(cli, "Ambiguous filter \"%s\" (begin, between)", argv[0]);
						return CLI_ERROR;
					}
					*filt = calloc(1, sizeof(struct cli_filter));

					if (!strncmp("include", argv[0], len) ||
						!strncmp("exclude", argv[0], len) ||
						!strncmp("grep", argv[0], len) ||
						!strncmp("egrep", argv[0], len))
							rc = cli_match_filter_init(cli, argc, argv, *filt);
					else if (!strncmp("begin", argv[0], len) ||
						!strncmp("between", argv[0], len))
							rc = cli_range_filter_init(cli, argc, argv, *filt);
					else if (!strncmp("count", argv[0], len))
						rc = cli_count_filter_init(cli, argc, argv, *filt);
					else
					{
						cli_error(cli, "Invalid filter \"%s\"", argv[0]);
						rc = CLI_ERROR;
					}

					if (rc == CLI_OK)
					{
						filt = &(*filt)->next;
					}
					else
					{
					free_z(*filt);
						*filt = 0;
					}
				}
*/
				if (rc == cliState.CLI_OK )
				{
					// 我们应该在这里转换mode
					cli_set_configmode(mode, null, null);
					rc = c.callback.call(this, c.command, words,  start_word + 1, (c_words - start_word - 1)+wilds);
				}
/*
				while (cli->common->filters)
				{
					struct cli_filter *filt = cli->common->filters;

					// call one last time to clean up
					filt->filter(cli, NULL, filt->data);
					cli->common->filters = filt->next;
					free(filt);
				}
*/
				return rc;
			}
			else if( isSuperMode(mode.parent, c) )
			{
				// again set and again is submode of c, we discard it.
				if( (again == null) || !isSuperMode(again.mode, c))
					// command matched but from another mode,
					// remember it if we fail to find correct command
					again = c;
			}
	    }
	    // drop out of config submode if we have matched command on MODE_CONFIG
	   	if(again!=null && (mode.parent != null) && (mode.parent != MODE_EXEC) )
		{
			c = again;
			return cli_find_command(mode.parent, c.children, c.callback, words, start_word + 1, filters,wilds, auto_hide_commands);
	    }
		cli_error("\r\n Invalid input '%s'", words.get(start_word));

	    return cliState.CLI_ERROR_ARG;
	}
	private boolean strncasecmp(String a, String b, int len) {
		boolean ret = (a.regionMatches(true, 0, b, 0, len));
		// System.out.println("cmp:" + a + " with:" + b + " len:" + len + " is:" + (ret ? " true" : " false"));
		return !ret;
	}
	private boolean link_hide_command(hide_command hideCommand, cli_command command) {
		return false;
	}
	private void cli_add_history(byte cmd[]) {
		
	}
	private boolean isSuperMode(cliMode mode, cli_command command) {
		return false;
	}
	private long time() {
		return System.currentTimeMillis() / 100000;
	}
	
	private int cli_get_completions() {
		return 0;
	}
	
	public void print(String format, Object ...args) throws IOException {
		write(String.format(format+"\r\n", args));
	}

	private int get_unique_len(cli_command c1, cli_command c2) {
		return Math.min(c1.command.length(), c2.command.length());
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
	
	IHelper_print _helper_print = null;
	
	IHelper_print helper_print = new IHelper_print() {

		@Override
		public void print(String format, Object... args) throws IOException {
			write(String.format(format+"\r\n", args));
		}
	};
	
	private void cli_print_callback(IHelper_print helper_print) {
		_helper_print = helper_print;
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



	public cli_command cli_register_command(cli_command parent, String command,
			CliCallback callback, int privilege, cliMode mode, String help)
		{
		    cli_command c, p;

		    if (command == null) return null;
		    c = new cli_command();
		    
		    c.callback = callback;
		    c.next = null;
			c.wilds_callback = null;
			c.command = command;
		    c.privilege = privilege;
		    c.mode = mode;
	    	c.help=help;

	    	cli_command r = null;
	    	if(parent == null)
	    		if(common.commands == null)
	    			common.commands = c;
	    		else
	    			r = common.commands;
	    	else
	    		if(parent.children == null)
	    			parent.children = c;
	    		else
	    			r = parent.children;
	    	
	    		/*
		    r = ( parent == null ) ? common.commands : parent.children;
		
			if (r==null)
			{
				r = c;
			}
			else
			*/
	    	if(r != null)
			{
				for (p = r; p!=null && p.next!=null; p = p.next);
				if (p!=null) p.next = c;
			}
		    return c;
		}
}
