package org.syncany.connection.plugins.bt;

import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;

public class BtPlugin extends Plugin {
	public static final String ID = "bt";

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return "BT";
	}

	@Override
	public Integer[] getVersion() {
		return new Integer[] { 0, 1 };
	}

	@Override
	public Connection createConnection() {
		return new BtConnection();
	}
}
