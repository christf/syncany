/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.tests.connection.plugins.ftp;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.syncany.tests.util.TestFileUtil;

/**
 * @author vincent
 *
 */
public class EmbeddedTestFtpServer {
	// FTP Server
	public static String HOST = "127.0.0.1";
	public static String HOST_WRONG = "noHost";
	
	public static int PORT = 2221;

	public static String USER1 = "user";
	public static String USER2 = "otherUser";
	public static String PASSWORD1 = "password";
	
	private static FtpServer server;
	private static File rootDir;
	
	public static void stopServer() {
		server.stop();
		rootDir.delete();
	}
	
	public static void startServer() throws Exception {
		rootDir = TestFileUtil.createTempDirectoryInSystemTemp();
		rootDir.deleteOnExit();

		PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
		userManagerFactory.setFile(new File("user.properties"));
		userManagerFactory.setPasswordEncryptor(new SaltedPasswordEncryptor());
		UserManager um = userManagerFactory.createUserManager();
		
		BaseUser user1 = new BaseUser();
		user1.setName(USER1);
		user1.setPassword(PASSWORD1);
		user1.setHomeDirectory(rootDir.getAbsolutePath());
		List<Authority> authorities = new ArrayList<Authority>();
		authorities.add(new WritePermission());
		user1.setAuthorities(authorities);
		
		um.save(user1);

		FtpServerFactory serverFactory = new FtpServerFactory();
		serverFactory.setUserManager(um);
		ListenerFactory factory = new ListenerFactory();
		factory.setPort(PORT);
		serverFactory.addListener("default", factory.createListener());
		NativeFileSystemFactory nfs = new NativeFileSystemFactory();
		nfs.setCaseInsensitive(false);
		nfs.setCreateHome(true);
		serverFactory.setFileSystem(nfs);
		server = serverFactory.createServer();

		server.start();
	}
	
	// FTP Client
	public static void mkdir(String path, String user) throws SocketException, IOException {
		FTPClient ftp = new FTPClient();
		ftp.setConnectTimeout(3000);
		ftp.setDataTimeout(3000);
		ftp.setDefaultTimeout(3000);

		ftp.connect(HOST, PORT);
		ftp.login(user, PASSWORD1);
		ftp.enterLocalPassiveMode();
		ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!
		ftp.makeDirectory(path);

		ftp.disconnect();
		ftp = null;
	}
}
