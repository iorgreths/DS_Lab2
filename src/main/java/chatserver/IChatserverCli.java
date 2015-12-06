package chatserver;

import java.io.IOException;

import cli.Command;

public interface IChatserverCli {

	// --- Commands needed for Lab 1 ---

	/**
	 * Prints out some information about each user, containing username, login
	 * status (online/offline)<br/>
	 * 
	 * @return the user information
	 * @throws IOException
	 */
	@Command
	public String users() throws IOException;

	/**
	 * Performs a shutdown of the chatserver and releases all resources. <br/>
	 * Shutting down an already terminated chatserver has no effect.
	 *
	 * @return any message indicating that the chatserver is going to terminate
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Command
	public String exit() throws IOException;

}
